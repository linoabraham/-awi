ALTER TABLE clientes ADD COLUMN codigo_referido VARCHAR(20) NULL;
UPDATE clientes SET codigo_referido = CONCAT('NAW', id_cliente) WHERE codigo_referido IS NULL;
ALTER TABLE clientes MODIFY COLUMN codigo_referido VARCHAR(20) NOT NULL;

CREATE TABLE billeteras_miplata (
  id_billetera_miplata BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_cliente BIGINT NOT NULL,
  saldo_disponible DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  moneda VARCHAR(3) NOT NULL DEFAULT 'PEN',
  version BIGINT NOT NULL DEFAULT 0,
  fecha_creacion DATETIME NOT NULL,
  fecha_actualizacion DATETIME NOT NULL,
  CONSTRAINT uk_billeteras_miplata_cliente UNIQUE (id_cliente),
  CONSTRAINT fk_billeteras_miplata_cliente FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente)
);

INSERT INTO billeteras_miplata (id_cliente, saldo_disponible, moneda, version, fecha_creacion, fecha_actualizacion)
SELECT id_cliente, 0.00, 'PEN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM clientes
WHERE id_cliente NOT IN (SELECT id_cliente FROM billeteras_miplata);

CREATE TABLE solicitudes_recarga_miplata (
  id_solicitud_recarga_miplata BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_cliente BIGINT NOT NULL,
  monto_soles DECIMAL(12,2) NOT NULL,
  comprobante_base64 LONGTEXT NOT NULL,
  codigo_referido_ingresado VARCHAR(20) NULL,
  id_cliente_referido BIGINT NULL,
  estado VARCHAR(20) NOT NULL,
  motivo_rechazo VARCHAR(500) NULL,
  revisado_por BIGINT NULL,
  fecha_revision DATETIME NULL,
  fecha_creacion DATETIME NOT NULL,
  fecha_actualizacion DATETIME NOT NULL,
  CONSTRAINT fk_solicitudes_recarga_miplata_cliente FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente),
  CONSTRAINT fk_solicitudes_recarga_miplata_referido FOREIGN KEY (id_cliente_referido) REFERENCES clientes(id_cliente),
  CONSTRAINT fk_solicitudes_recarga_miplata_revisor FOREIGN KEY (revisado_por) REFERENCES usuarios(id_usuario)
);
CREATE INDEX idx_solicitudes_recarga_miplata_cliente ON solicitudes_recarga_miplata(id_cliente);
CREATE INDEX idx_solicitudes_recarga_miplata_estado ON solicitudes_recarga_miplata(estado);

CREATE TABLE movimientos_miplata (
  id_movimiento_miplata BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_billetera_miplata BIGINT NOT NULL,
  id_cliente BIGINT NOT NULL,
  id_solicitud_recarga_miplata BIGINT NULL,
  id_venta_credito BIGINT NULL,
  id_membresia BIGINT NULL,
  tipo_movimiento VARCHAR(30) NOT NULL,
  monto_soles DECIMAL(12,2) NOT NULL,
  saldo_anterior DECIMAL(12,2) NOT NULL,
  saldo_posterior DECIMAL(12,2) NOT NULL,
  moneda VARCHAR(3) NOT NULL DEFAULT 'PEN',
  descripcion VARCHAR(300) NOT NULL,
  registrado_por BIGINT NULL,
  fecha_creacion DATETIME NOT NULL,
  CONSTRAINT fk_movimientos_miplata_billetera FOREIGN KEY (id_billetera_miplata) REFERENCES billeteras_miplata(id_billetera_miplata),
  CONSTRAINT fk_movimientos_miplata_cliente FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente),
  CONSTRAINT fk_movimientos_miplata_solicitud FOREIGN KEY (id_solicitud_recarga_miplata) REFERENCES solicitudes_recarga_miplata(id_solicitud_recarga_miplata),
  CONSTRAINT fk_movimientos_miplata_venta FOREIGN KEY (id_venta_credito) REFERENCES ventas_creditos(id_venta_credito),
  CONSTRAINT fk_movimientos_miplata_membresia FOREIGN KEY (id_membresia) REFERENCES membresias(id_membresia),
  CONSTRAINT fk_movimientos_miplata_registrador FOREIGN KEY (registrado_por) REFERENCES usuarios(id_usuario)
);
CREATE INDEX idx_movimientos_miplata_cliente ON movimientos_miplata(id_cliente, fecha_creacion);

CREATE UNIQUE INDEX uk_clientes_codigo_referido ON clientes(codigo_referido);
