-- Auditoría de seguridad: rastro de eventos sensibles por usuario (inicios de
-- sesión, dispositivos nuevos, cierres remotos, cambio de clave, etc.).
CREATE TABLE eventos_seguridad (
  id_evento_seguridad BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_usuario BIGINT NOT NULL,
  tipo VARCHAR(40) NOT NULL,
  installation_id VARCHAR(64) NULL,
  direccion_ip VARCHAR(45) NULL,
  detalle VARCHAR(255) NULL,
  fecha_creacion DATETIME NOT NULL,
  CONSTRAINT fk_evento_seguridad_usuario
    FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario)
);

CREATE INDEX idx_eventos_seguridad_usuario_fecha
  ON eventos_seguridad (id_usuario, fecha_creacion);
