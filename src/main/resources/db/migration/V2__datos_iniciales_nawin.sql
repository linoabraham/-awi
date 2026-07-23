-- Inserts sin cambios
INSERT INTO roles (codigo, nombre, activo) VALUES
                                               ('ADMIN', 'Administrador', TRUE),
                                               ('TRABAJADOR', 'Trabajador', TRUE),
                                               ('CLIENTE', 'Cliente', TRUE);

INSERT INTO endpoints_busqueda
(codigo, nombre, descripcion, metodo_proveedor, ruta_proveedor, parametro_principal, tipo_consumo_proveedor, costo_proveedor, es_critico, activo, fecha_creacion, fecha_actualizacion)
VALUES
    ('RUC', 'RUC', 'Consulta razón social, estado fiscal, dirección y datos comerciales.', 'GET', '/api/v1/consultas/sunat/ruc/{ruc}', 'ruc', 'PETICION', 1, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DNI_BASICO', 'DNI básico', 'Consulta nombres a partir de un DNI válido.', 'GET', '/api/v1/consultas/reniec/dni/{dni}', 'dni', 'PETICION', 1, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DNIV', 'DNI con imágenes', 'Consulta el DNI con datos básicos e imágenes del documento.', 'GET', '/api/v1/consultas/fd/dniv/{dni}', 'dni', 'CREDITO', 8, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DNIVEL', 'DNI electrónico', 'Consulta DNI electrónico.', 'GET', '/api/v1/consultas/fd/dnivel/{dni}', 'dni', 'CREDITO', 8, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DNI_COMPLETO', 'DNI completo', 'Consulta información completa del DNI.', 'GET', '/api/v1/consultas/fd/dni/{dni}', 'dni', 'CREDITO', 2, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DNIT', 'DNI con huellas y firma', 'Consulta DNI con huellas y firma.', 'GET', '/api/v1/consultas/fd/dnit/{dni}', 'dni', 'CREDITO', 5, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('NM', 'Búsqueda por nombres', 'Consulta personas por nombres y apellidos.', 'GET', '/api/v1/consultas/fd/nm', 'nombres', 'CREDITO', 4, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('AG', 'Familiares y relaciones', 'Consulta familiares y relaciones por DNI.', 'GET', '/api/v1/consultas/fd/ag/{dni}', 'dni', 'CREDITO', 8, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TELP_DNI', 'Líneas por DNI', 'Consulta líneas telefónicas por DNI.', 'GET', '/api/v1/consultas/fd/telp/{dni}', 'dni', 'CREDITO', 15, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TELP_CEL', 'Titular por celular', 'Consulta titular por número celular.', 'GET', '/api/v1/consultas/fd/telp/cel/{numero}', 'numero', 'CREDITO', 15, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DEN', 'Resumen de denuncias', 'Consulta resumen de denuncias asociadas a un DNI.', 'GET', '/api/v1/consultas/fd/den/{dni}', 'dni', 'CREDITO', 15, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DENUNCIAS', 'PDF de denuncias', 'Consulta documentos PDF de denuncias asociadas a un DNI.', 'GET', '/api/v1/consultas/fd/denuncias/{dni}', 'dni', 'CREDITO', 30, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('RQH', 'Requisitorias y PDF', 'Consulta requisitorias y documentos PDF por DNI.', 'GET', '/api/v1/consultas/fd/rqh/{dni}', 'dni', 'CREDITO', 30, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('FACIAL_TOP', 'Coincidencia facial', 'Envía una imagen facial y devuelve las coincidencias más altas.', 'POST', '/api/v1/consultas/fd/facial/top', 'image_facial', 'CREDITO', 30, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('PLA', 'Datos de placa', 'Consulta datos completos de una placa vehicular.', 'GET', '/api/v1/consultas/fd/pla/{placa}', 'placa', 'CREDITO', 2, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DENPLA', 'Denuncias PDF por placa', 'Consulta denuncias policiales PDF asociadas a una placa vehicular.', 'GET', '/api/v1/consultas/fd/denpla/{placa}', 'placa', 'CREDITO', 30, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('PLAT', 'Vehículo y propietarios', 'Consulta datos completos del vehículo y propietarios por placa.', 'GET', '/api/v1/consultas/fd/plat/{placa}', 'placa', 'CREDITO', 5, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('HSOAT', 'Historial SOAT', 'Consulta historial SOAT completo de una placa vehicular.', 'GET', '/api/v1/consultas/fd/hsoat/{placa}', 'placa', 'CREDITO', 8, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO series_comprobantes
(tipo_comprobante, serie, ultimo_numero, activo, fecha_creacion, fecha_actualizacion)
VALUES
    ('BOLETA', 'B001', 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('FACTURA', 'F001', 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('RECIBO_INTERNO', 'R001', 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);