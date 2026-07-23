-- Verificación de correo para registro público de clientes y reset de clave.
-- Los usuarios existentes quedan marcados como verificados para no bloquear su acceso.
ALTER TABLE usuarios ADD COLUMN correo_verificado BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE codigos_verificacion (
    id_codigo BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    codigo_hash VARCHAR(64) NOT NULL,
    fecha_expiracion DATETIME NOT NULL,
    intentos INT NOT NULL DEFAULT 0,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME NOT NULL,
    CONSTRAINT fk_codver_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);
CREATE INDEX idx_codver_usuario_tipo ON codigos_verificacion(id_usuario, tipo, usado);
