-- Métodos de pago configurables desde el panel (Yape ahora; Izipay u otros a
-- futuro con sus credenciales). Reemplaza a configuracion_yape.
CREATE TABLE metodos_pago (
  id_metodo_pago BIGINT AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(30) NOT NULL UNIQUE,
  nombre VARCHAR(80) NOT NULL,
  tipo VARCHAR(30) NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  orden INT NOT NULL DEFAULT 0,
  numero VARCHAR(20) NULL,
  titular VARCHAR(120) NULL,
  qr_base64 LONGTEXT NULL,
  credenciales_json LONGTEXT NULL,
  instrucciones VARCHAR(500) NULL,
  validacion_texto VARCHAR(60) NOT NULL DEFAULT '5 a 10 min',
  actualizado_por BIGINT NULL,
  fecha_creacion DATETIME NOT NULL,
  fecha_actualizacion DATETIME NOT NULL,
  CONSTRAINT fk_metodos_pago_usuario FOREIGN KEY (actualizado_por) REFERENCES usuarios(id_usuario)
);

-- Migra la configuración Yape existente a un método de pago.
INSERT INTO metodos_pago (codigo, nombre, tipo, activo, orden, numero, titular, qr_base64, validacion_texto, fecha_creacion, fecha_actualizacion)
SELECT 'YAPE', 'Yape', 'BILLETERA_QR', TRUE, 0, numero, titular, qr_base64, validacion_texto, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM configuracion_yape
ORDER BY id_configuracion_yape ASC
LIMIT 1;

-- Si no había configuración previa, siembra un método Yape por defecto.
INSERT INTO metodos_pago (codigo, nombre, tipo, activo, orden, numero, titular, validacion_texto, fecha_creacion, fecha_actualizacion)
SELECT 'YAPE', 'Yape', 'BILLETERA_QR', TRUE, 0, '900123456', 'Ñawi S.A.C.', '5 a 10 min', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM metodos_pago WHERE codigo = 'YAPE');

DROP TABLE configuracion_yape;
