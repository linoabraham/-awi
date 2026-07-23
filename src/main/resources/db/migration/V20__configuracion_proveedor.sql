-- Credenciales del proveedor CODART editables desde el panel (sin reiniciar el
-- backend). El token se guarda CIFRADO. Si la fila está vacía, se usa el valor
-- de las variables de entorno / application.yml (fallback).
CREATE TABLE configuracion_proveedor (
  id_configuracion_proveedor BIGINT AUTO_INCREMENT PRIMARY KEY,
  api_token_cifrado TEXT NULL,
  base_url VARCHAR(255) NULL,
  fecha_actualizacion DATETIME NOT NULL
);

INSERT INTO configuracion_proveedor (api_token_cifrado, base_url, fecha_actualizacion)
VALUES (NULL, NULL, CURRENT_TIMESTAMP);
