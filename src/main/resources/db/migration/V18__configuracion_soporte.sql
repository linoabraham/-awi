-- Canales oficiales de soporte y redes de la app (editables desde el panel).
-- Una sola fila: WhatsApp, correo, redes sociales y enlaces informativos.
CREATE TABLE configuracion_soporte (
  id_configuracion_soporte BIGINT AUTO_INCREMENT PRIMARY KEY,
  whatsapp VARCHAR(20) NULL,
  correo VARCHAR(120) NULL,
  facebook VARCHAR(255) NULL,
  instagram VARCHAR(255) NULL,
  tiktok VARCHAR(255) NULL,
  terminos_url VARCHAR(255) NULL,
  privacidad_url VARCHAR(255) NULL,
  fecha_actualizacion DATETIME NOT NULL
);

INSERT INTO configuracion_soporte
  (whatsapp, correo, facebook, instagram, tiktok, terminos_url, privacidad_url, fecha_actualizacion)
VALUES
  (NULL, 'soporte@nawi.app', NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP);
