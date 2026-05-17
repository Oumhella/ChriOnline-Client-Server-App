#!/bin/sh
# ═══════════════════════════════════════════════════════════════
# Script d'initialisation automatique de HashiCorp Vault
# Configure : AppRole, KV v2, PKI, Transit
# ═══════════════════════════════════════════════════════════════

set -e

VAULT_ADDR="http://vault:8200"
export VAULT_ADDR

echo "══════════════════════════════════════════════════════"
echo "  ChriOnline — Initialisation Vault (AppRole + Transit)"
echo "══════════════════════════════════════════════════════"

# ── 1. Attendre que Vault soit prêt ──────────────────────────
echo "[1/8] Attente du démarrage de Vault..."
until vault status 2>/dev/null | grep -q "Initialized"; do
  sleep 2
  echo "  ... Vault pas encore prêt, nouvelle tentative..."
done

# ── 2. Vérifier si Vault est déjà initialisé ─────────────────
INITIALIZED=$(vault status -format=json 2>/dev/null | grep '"initialized"' | grep -o 'true\|false')

if [ "$INITIALIZED" = "false" ]; then
  echo "[2/8] Initialisation de Vault (1 clé, seuil 1)..."
  INIT_OUTPUT=$(vault operator init -key-shares=1 -key-threshold=1 -format=json)
  
  UNSEAL_KEY=$(echo "$INIT_OUTPUT" | awk '/"unseal_keys_b64":/ {getline; gsub(/[ ",]/, ""); print}')
  ROOT_TOKEN=$(echo "$INIT_OUTPUT" | awk -F '"' '/"root_token":/ {print $4}')
  
  echo ""
  echo "  ╔═══════════════════════════════════════════════════╗"
  echo "  ║  IMPORTANT : SAUVEGARDEZ CES INFORMATIONS !      ║"
  echo "  ╠═══════════════════════════════════════════════════╣"
  echo "  ║  Unseal Key : $UNSEAL_KEY"
  echo "  ║  Root Token : $ROOT_TOKEN"
  echo "  ╚═══════════════════════════════════════════════════╝"
  echo ""
  
  # Sauvegarder dans un fichier pour référence
  echo "$UNSEAL_KEY" > /vault/data/unseal_key.txt
  echo "$ROOT_TOKEN" > /vault/data/root_token.txt
  
  echo "[3/8] Déverrouillage (Unseal) de Vault..."
  vault operator unseal "$UNSEAL_KEY"
else
  echo "[2/8] Vault déjà initialisé."
  
  # Lire la clé de déverrouillage sauvegardée
  if [ -f /vault/data/unseal_key.txt ]; then
    UNSEAL_KEY=$(cat /vault/data/unseal_key.txt)
    ROOT_TOKEN=$(cat /vault/data/root_token.txt)
    
    # Vérifier si Vault est scellé
    SEALED=$(vault status -format=json 2>/dev/null | grep '"sealed"' | grep -o 'true\|false')
    if [ "$SEALED" = "true" ]; then
      echo "[3/8] Déverrouillage de Vault..."
      vault operator unseal "$UNSEAL_KEY"
    else
      echo "[3/8] Vault déjà déverrouillé."
    fi
  else
    echo "[ERREUR] Fichier unseal_key.txt introuvable. Veuillez réinitialiser Vault."
    exit 1
  fi
fi

# Authentification avec le Root Token
export VAULT_TOKEN="$ROOT_TOKEN"
vault login "$ROOT_TOKEN" > /dev/null 2>&1

# ── 4. Activer le moteur KV v2 (Stockage des clés) ──────────
echo "[4/8] Activation du moteur KV v2..."
vault secrets enable -path=secret -version=2 kv 2>/dev/null || echo "  (KV v2 déjà activé)"

# Stocker le mot de passe du keystore admin
vault kv put secret/admin/keystore password="testPass123"
echo "  → Secret admin/keystore enregistré."

# ── 5. Activer le moteur PKI (Certificats SSL) ──────────────
echo "[5/8] Activation du moteur PKI..."
vault secrets enable pki 2>/dev/null || echo "  (PKI déjà activé)"
vault secrets tune -max-lease-ttl=87600h pki 2>/dev/null

# Générer le certificat Root CA
vault write pki/root/generate/internal \
  common_name="ChriOnline Root CA" \
  ttl=87600h > /dev/null 2>&1 || echo "  (Root CA déjà généré)"

# Configurer les URLs
vault write pki/config/urls \
  issuing_certificates="$VAULT_ADDR/v1/pki/ca" \
  crl_distribution_points="$VAULT_ADDR/v1/pki/crl" > /dev/null 2>&1

# Créer le rôle pour générer des certificats serveur
vault write pki/roles/chrionline-server \
  allowed_domains="localhost,chrionline.com" \
  allow_subdomains=true \
  allow_localhost=true \
  max_ttl=72h > /dev/null 2>&1
echo "  → PKI configuré avec le rôle 'chrionline-server'."

# ── 6. Activer le moteur Transit (Chiffrement) ──────────────
echo "[6/8] Activation du moteur Transit..."
vault secrets enable transit 2>/dev/null || echo "  (Transit déjà activé)"

# Créer une clé de chiffrement pour les données sensibles
vault write -f transit/keys/chrionline-data > /dev/null 2>&1 || echo "  (Clé transit déjà créée)"
echo "  → Clé de chiffrement 'chrionline-data' créée."

# ── 7. Configurer l'authentification AppRole ─────────────────
echo "[7/8] Configuration de l'authentification AppRole..."
vault auth enable approle 2>/dev/null || echo "  (AppRole déjà activé)"

# Créer la politique d'accès pour l'application ChriOnline
vault policy write chrionline-policy - <<EOF
# ── Politique de sécurité ChriOnline ──

# Lecture/écriture des secrets KV (clés publiques admins, config)
path "secret/data/*" {
  capabilities = ["create", "read", "update", "list"]
}
path "secret/metadata/*" {
  capabilities = ["list", "read"]
}

# Génération de certificats SSL via PKI
path "pki/issue/chrionline-server" {
  capabilities = ["create", "update"]
}
path "pki/ca/pem" {
  capabilities = ["read"]
}

# Chiffrement/Déchiffrement via Transit
path "transit/encrypt/chrionline-data" {
  capabilities = ["update"]
}
path "transit/decrypt/chrionline-data" {
  capabilities = ["update"]
}

# Lecture du statut de santé
path "sys/health" {
  capabilities = ["read"]
}
EOF
echo "  → Politique 'chrionline-policy' créée."

# Créer le rôle AppRole avec la politique
vault write auth/approle/role/chrionline-app \
  token_policies="chrionline-policy" \
  token_ttl=1h \
  token_max_ttl=4h \
  secret_id_ttl=0 \
  secret_id_num_uses=0 > /dev/null 2>&1

# Récupérer le RoleID et générer un SecretID
ROLE_ID=$(vault read -field=role_id auth/approle/role/chrionline-app/role-id)
SECRET_ID=$(vault write -f -field=secret_id auth/approle/role/chrionline-app/secret-id)

# ── 8. Affichage final ───────────────────────────────────────
echo "[8/8] Configuration terminée !"
echo ""
echo "╔═══════════════════════════════════════════════════════════╗"
echo "║        VAULT CONFIGURÉ AVEC SUCCÈS                      ║"
echo "╠═══════════════════════════════════════════════════════════╣"
echo "║                                                         ║"
echo "║  Configurez ces variables d'environnement :             ║"
echo "║                                                         ║"
echo "║  VAULT_ROLE_ID=$ROLE_ID"
echo "║  VAULT_SECRET_ID=$SECRET_ID"
echo "║                                                         ║"
echo "║  Moteurs activés :                                      ║"
echo "║    ✓ KV v2     (secret/)     — Stockage clés RSA        ║"
echo "║    ✓ PKI       (pki/)        — Certificats SSL          ║"
echo "║    ✓ Transit   (transit/)    — Chiffrement données      ║"
echo "║    ✓ AppRole   (auth/)       — Authentification app     ║"
echo "║                                                         ║"
echo "╚═══════════════════════════════════════════════════════════╝"

# Sauvegarder les IDs pour référence
echo "$ROLE_ID" > /vault/data/role_id.txt
echo "$SECRET_ID" > /vault/data/secret_id.txt

echo ""
echo "[VAULT] Prêt à recevoir des connexions AppRole."
