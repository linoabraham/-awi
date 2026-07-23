-- Configuración de la cuenta Yape del comercio (número, titular, QR).
-- Editable únicamente por un administrador desde el panel.
CREATE TABLE configuracion_yape (
  id_configuracion_yape BIGINT AUTO_INCREMENT PRIMARY KEY,
  numero VARCHAR(20) NOT NULL,
  titular VARCHAR(120) NOT NULL,
  qr_base64 LONGTEXT NULL,
  validacion_texto VARCHAR(60) NOT NULL DEFAULT '5 a 10 min',
  actualizado_por BIGINT NULL,
  fecha_creacion DATETIME NOT NULL,
  fecha_actualizacion DATETIME NOT NULL,
  CONSTRAINT fk_configuracion_yape_usuario FOREIGN KEY (actualizado_por) REFERENCES usuarios(id_usuario)
);

INSERT INTO configuracion_yape (numero, titular, qr_base64, validacion_texto, fecha_creacion, fecha_actualizacion)
VALUES ('900123456', 'Ñawi S.A.C.', NULL, '5 a 10 min', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Órdenes de pago: código de compra, expiración a 24h y datos del comprobante.
-- El comprobante deja de ser obligatorio al crear la orden (se adjunta luego).
ALTER TABLE solicitudes_recarga_miplata
  MODIFY COLUMN comprobante_base64 LONGTEXT NULL;

ALTER TABLE solicitudes_recarga_miplata
  ADD COLUMN codigo_orden VARCHAR(30) NULL,
  ADD COLUMN codigo_operacion VARCHAR(60) NULL,
  ADD COLUMN concepto VARCHAR(200) NULL,
  ADD COLUMN id_plan BIGINT NULL,
  ADD COLUMN id_paquete_credito BIGINT NULL,
  ADD COLUMN fecha_expiracion DATETIME NULL;

ALTER TABLE solicitudes_recarga_miplata
  ADD CONSTRAINT uk_solicitudes_recarga_codigo_orden UNIQUE (codigo_orden);
