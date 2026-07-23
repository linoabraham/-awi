-- Anti-abuso de referidos:
-- (1) El bono del referente queda PENDIENTE hasta la primera consulta exitosa
--     del invitado (los canjes históricos ya fueron acreditados).
-- (2) Cada canje registra el dispositivo (installation_id) e IP para permitir
--     un solo canje por dispositivo y auditar granjas de cuentas.
ALTER TABLE canjes_referido
  ADD COLUMN installation_id VARCHAR(64) NULL,
  ADD COLUMN direccion_ip VARCHAR(45) NULL,
  ADD COLUMN estado_bono_referente VARCHAR(20) NOT NULL DEFAULT 'ACREDITADO',
  ADD COLUMN fecha_acreditacion DATETIME NULL;

CREATE INDEX idx_canjes_referido_dispositivo ON canjes_referido(installation_id);

-- (3) Correo normalizado (anti alias de Gmail): minúsculas, sin sufijo "+algo";
--     en Gmail además sin puntos en la parte local.
ALTER TABLE usuarios
  ADD COLUMN correo_normalizado VARCHAR(160) NULL;

UPDATE usuarios SET correo_normalizado = LOWER(
  CASE
    WHEN LOWER(SUBSTRING_INDEX(correo, '@', -1)) IN ('gmail.com', 'googlemail.com')
      THEN CONCAT(
        REPLACE(SUBSTRING_INDEX(SUBSTRING_INDEX(correo, '@', 1), '+', 1), '.', ''),
        '@', SUBSTRING_INDEX(correo, '@', -1))
    ELSE CONCAT(
        SUBSTRING_INDEX(SUBSTRING_INDEX(correo, '@', 1), '+', 1),
        '@', SUBSTRING_INDEX(correo, '@', -1))
  END);

CREATE INDEX idx_usuarios_correo_normalizado ON usuarios(correo_normalizado);
