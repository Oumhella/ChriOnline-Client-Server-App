param (
    [Parameter(Position=0, Mandatory=$false)]
    [string]$Ciphertext,
    
    [switch]$ShowAll
)

# 1. Charger le Root Token depuis le fichier local
$TokenFile = "vault/data/root_token.txt"
if (-not (Test-Path $TokenFile)) {
    Write-Error "Fichier root_token.txt introuvable dans vault/data/ !"
    exit 1
}
$VaultToken = (Get-Content $TokenFile).Trim()

# Fonction interne pour déchiffrer un unique ciphertext via Docker Vault
function Decrypt-Ciphertext($cipher) {
    $Result = docker exec -e VAULT_TOKEN="$VaultToken" vault-prod vault write -format=json transit/decrypt/chrionline-data ciphertext="$cipher" 2>$null
    if ($null -ne $Result -and $Result -ne "") {
        try {
            $Json = $Result | ConvertFrom-Json
            $Base64 = $Json.data.plaintext
            return [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($Base64))
        } catch {
            return $null
        }
    }
    return $null
}

# --- MODE 1 : Si aucun paramètre n'est passé ou si -ShowAll est activé ---
# Déchiffre et affiche tout le fichier logs/app.log d'un coup !
if ($ShowAll -or ($null -eq $Ciphertext -or $Ciphertext -eq "")) {
    $LogFile = "logs/app.log"
    if (-not (Test-Path $LogFile)) {
        Write-Error "Fichier logs/app.log introuvable."
        exit 1
    }
    
    Write-Host "Lecture et déchiffrement en direct de : $LogFile ...`n" -ForegroundColor Cyan
    Write-Host "=================== FLUX DE LOGS CHRIONLINE ===================" -ForegroundColor Green
    
    Get-Content $LogFile | ForEach-Object {
        $Line = $_
        # Détection d'un log crypté vault:v1:...
        if ($Line -match "\[SECURE_ENCRYPTED\]\s*(vault:v1:[A-Za-z0-9+/=]+)") {
            $Cipher = $Matches[1]
            $Cleartext = Decrypt-Ciphertext $Cipher
            if ($null -ne $Cleartext) {
                # On remplace la partie chiffrée par le texte en clair
                $Line = $Line -replace "\[SECURE_ENCRYPTED\]\s*vault:v1:[A-Za-z0-9+/=]+", "[🔓 CLAIR] $Cleartext"
                Write-Host $Line -ForegroundColor Green
            } else {
                Write-Host "$Line (Échec déchiffrement Vault)" -ForegroundColor Red
            }
        } else {
            # Ligne de log normale non cryptée
            Write-Host $Line -ForegroundColor Gray
        }
    }
    Write-Host "==============================================================`n" -ForegroundColor Green
    exit 0
}

# --- MODE 2 : Déchiffrer un log unique passé en paramètre ---
Write-Host "Connexion sécurisée au conteneur Vault..." -ForegroundColor Cyan
$Decrypted = Decrypt-Ciphertext $Ciphertext

if ($null -ne $Decrypted) {
    Write-Host "`n==================================================" -ForegroundColor Green
    Write-Host "🔓 LOG DÉCHIFFRÉ EN CLAIR : " -ForegroundColor Green -NoNewline
    Write-Host $Decrypted -ForegroundColor White
    Write-Host "==================================================`n" -ForegroundColor Green
} else {
    Write-Error "Erreur lors du déchiffrement. Assurez-vous que Vault est actif et que le texte chiffré commence par 'vault:v1:'"
}
