-- ═══════════════════════════════════════════════════════════════
-- Nouvelles tables de sécurité — ChriOnline
-- À exécuter une seule fois sur la base de données
-- ═══════════════════════════════════════════════════════════════

-- ── 1. Table de persistance des sessions TCP ──────────────────
CREATE TABLE IF NOT EXISTS sessions (
    session_id    VARCHAR(100) NOT NULL PRIMARY KEY,
    user_id       INT          NOT NULL,
    ip_address    VARCHAR(45),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at    TIMESTAMP    NOT NULL,
    actif         BOOLEAN      NOT NULL DEFAULT TRUE,
    INDEX idx_sessions_user  (user_id),
    INDEX idx_sessions_actif (actif, expires_at),
    FOREIGN KEY (user_id) REFERENCES utilisateur(idUtilisateur) ON DELETE CASCADE
);

-- ── 2. Table liste noire IP ───────────────────────────────────
CREATE TABLE IF NOT EXISTS security_blacklist (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    ip_address  VARCHAR(45)  NOT NULL,
    email       VARCHAR(255),
    raison      VARCHAR(255),
    date_ajout  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expire_le   TIMESTAMP    NOT NULL,
    actif       BOOLEAN      NOT NULL DEFAULT TRUE,
    INDEX idx_blacklist_ip     (ip_address),
    INDEX idx_blacklist_actif  (actif, expire_le)
);
