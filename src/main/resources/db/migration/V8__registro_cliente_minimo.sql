-- Registro público simplificado: el cliente se crea solo con nombre, correo y
-- clave. El documento, celular y demás datos se completan luego en el perfil,
-- por eso estas columnas pasan a ser opcionales.
ALTER TABLE usuarios MODIFY celular VARCHAR(15) NULL;
ALTER TABLE clientes MODIFY tipo_documento VARCHAR(10) NULL;
ALTER TABLE clientes MODIFY numero_documento VARCHAR(11) NULL;
