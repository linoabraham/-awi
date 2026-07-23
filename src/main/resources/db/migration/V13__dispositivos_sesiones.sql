-- Dispositivos/sesiones: cada token de refresco representa una sesión ligada a
-- un dispositivo identificado por installation_id (UUID generado en la app).
-- Regla: máximo 2 dispositivos activos por usuario.
ALTER TABLE tokens_refresco
  ADD COLUMN installation_id VARCHAR(64) NULL,
  ADD COLUMN nombre_dispositivo VARCHAR(120) NULL,
  ADD COLUMN modelo VARCHAR(120) NULL,
  ADD COLUMN plataforma VARCHAR(20) NULL,
  ADD COLUMN ultima_actividad DATETIME NULL;

CREATE INDEX idx_tokens_refresco_usuario_dispositivo
  ON tokens_refresco(id_usuario, installation_id);
