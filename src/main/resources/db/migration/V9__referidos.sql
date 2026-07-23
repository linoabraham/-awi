-- Canjes de código de referido: registro para trazabilidad y estadísticas.
-- La data de respuesta de consultas vive en el dispositivo; aquí solo va la
-- información básica de auditoría del canje.
CREATE TABLE canjes_referido (
  id_canje_referido BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_cliente_referente BIGINT NOT NULL,
  id_cliente_invitado BIGINT NOT NULL,
  codigo_referido VARCHAR(20) NOT NULL,
  creditos_invitado INT NOT NULL,
  creditos_referente INT NOT NULL,
  fecha_canje DATETIME NOT NULL,
  CONSTRAINT uk_canjes_referido_invitado UNIQUE (id_cliente_invitado),
  CONSTRAINT fk_canjes_referido_referente FOREIGN KEY (id_cliente_referente) REFERENCES clientes(id_cliente),
  CONSTRAINT fk_canjes_referido_invitado FOREIGN KEY (id_cliente_invitado) REFERENCES clientes(id_cliente)
);
CREATE INDEX idx_canjes_referido_referente ON canjes_referido(id_cliente_referente);
