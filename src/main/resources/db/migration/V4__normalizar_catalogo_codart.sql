UPDATE endpoints_busqueda
SET metodo_proveedor = 'GET', ruta_proveedor = '/api/v1/consultas/sunat/ruc/{ruc}', parametro_principal = 'ruc',
    tipo_consumo_proveedor = 'PETICION', costo_proveedor = 1, activo = TRUE, fecha_actualizacion = CURRENT_TIMESTAMP
WHERE codigo = 'RUC';

UPDATE endpoints_busqueda
SET metodo_proveedor = 'GET', ruta_proveedor = '/api/v1/consultas/reniec/dni/{dni}', parametro_principal = 'dni',
    tipo_consumo_proveedor = 'PETICION', costo_proveedor = 1, activo = TRUE, fecha_actualizacion = CURRENT_TIMESTAMP
WHERE codigo = 'DNI_BASICO';

UPDATE endpoints_busqueda
SET metodo_proveedor = 'GET', ruta_proveedor = '/api/v1/consultas/fd/dniv/{dni}', parametro_principal = 'dni',
    tipo_consumo_proveedor = 'CREDITO', costo_proveedor = 8, activo = TRUE, fecha_actualizacion = CURRENT_TIMESTAMP
WHERE codigo = 'DNIV';

UPDATE endpoints_busqueda
SET metodo_proveedor = 'GET', ruta_proveedor = '/api/v1/consultas/fd/dnivel/{dni}', parametro_principal = 'dni',
    tipo_consumo_proveedor = 'CREDITO', costo_proveedor = 8, activo = TRUE, fecha_actualizacion = CURRENT_TIMESTAMP
WHERE codigo = 'DNIVEL';

UPDATE endpoints_busqueda
SET metodo_proveedor = 'GET', ruta_proveedor = '/api/v1/consultas/fd/dni/{dni}', parametro_principal = 'dni',
    tipo_consumo_proveedor = 'CREDITO', costo_proveedor = 2, activo = TRUE, fecha_actualizacion = CURRENT_TIMESTAMP
WHERE codigo = 'DNI_COMPLETO';

UPDATE endpoints_busqueda
SET metodo_proveedor = 'GET', ruta_proveedor = '/api/v1/consultas/fd/dnit/{dni}', parametro_principal = 'dni',
    tipo_consumo_proveedor = 'CREDITO', costo_proveedor = 5, activo = TRUE, fecha_actualizacion = CURRENT_TIMESTAMP
WHERE codigo = 'DNIT';

UPDATE endpoints_busqueda
SET metodo_proveedor = 'GET', ruta_proveedor = '/api/v1/consultas/fd/nm', parametro_principal = 'nombres',
    tipo_consumo_proveedor = 'CREDITO', costo_proveedor = 4, activo = TRUE, fecha_actualizacion = CURRENT_TIMESTAMP
WHERE codigo = 'NM';

UPDATE endpoints_busqueda
SET metodo_proveedor = 'GET', ruta_proveedor = '/api/v1/consultas/fd/ag/{dni}', parametro_principal = 'dni',
    tipo_consumo_proveedor = 'CREDITO', costo_proveedor = 8, activo = TRUE, fecha_actualizacion = CURRENT_TIMESTAMP
WHERE codigo = 'AG';

UPDATE endpoints_busqueda
SET metodo_proveedor = 'GET', ruta_proveedor = '/api/v1/consultas/fd/telp/{dni}', parametro_principal = 'dni',
    tipo_consumo_proveedor = 'CREDITO', costo_proveedor = 15, activo = TRUE, fecha_actualizacion = CURRENT_TIMESTAMP
WHERE codigo = 'TELP_DNI';

UPDATE endpoints_busqueda
SET metodo_proveedor = 'GET', ruta_proveedor = '/api/v1/consultas/fd/telp/cel/{numero}', parametro_principal = 'numero',
    tipo_consumo_proveedor = 'CREDITO', costo_proveedor = 15, activo = TRUE, fecha_actualizacion = CURRENT_TIMESTAMP
WHERE codigo = 'TELP_CEL';

UPDATE endpoints_busqueda
SET metodo_proveedor = 'GET', ruta_proveedor = '/api/v1/consultas/fd/den/{dni}', parametro_principal = 'dni',
    tipo_consumo_proveedor = 'CREDITO', costo_proveedor = 15, activo = TRUE, fecha_actualizacion = CURRENT_TIMESTAMP
WHERE codigo = 'DEN';

UPDATE endpoints_busqueda
SET metodo_proveedor = 'GET', ruta_proveedor = '/api/v1/consultas/fd/denuncias/{dni}', parametro_principal = 'dni',
    tipo_consumo_proveedor = 'CREDITO', costo_proveedor = 30, activo = TRUE, fecha_actualizacion = CURRENT_TIMESTAMP
WHERE codigo = 'DENUNCIAS';

UPDATE endpoints_busqueda
SET metodo_proveedor = 'GET', ruta_proveedor = '/api/v1/consultas/fd/rqh/{dni}', parametro_principal = 'dni',
    tipo_consumo_proveedor = 'CREDITO', costo_proveedor = 30, activo = TRUE, fecha_actualizacion = CURRENT_TIMESTAMP
WHERE codigo = 'RQH';

UPDATE endpoints_busqueda
SET metodo_proveedor = 'POST', ruta_proveedor = '/api/v1/consultas/fd/facial/top', parametro_principal = 'image_facial',
    tipo_consumo_proveedor = 'CREDITO', costo_proveedor = 30, activo = TRUE, fecha_actualizacion = CURRENT_TIMESTAMP
WHERE codigo = 'FACIAL_TOP';

UPDATE endpoints_busqueda
SET metodo_proveedor = 'GET', ruta_proveedor = '/api/v1/consultas/fd/pla/{placa}', parametro_principal = 'placa',
    tipo_consumo_proveedor = 'CREDITO', costo_proveedor = 2, activo = TRUE, fecha_actualizacion = CURRENT_TIMESTAMP
WHERE codigo = 'PLA';

UPDATE endpoints_busqueda
SET metodo_proveedor = 'GET', ruta_proveedor = '/api/v1/consultas/fd/denpla/{placa}', parametro_principal = 'placa',
    tipo_consumo_proveedor = 'CREDITO', costo_proveedor = 30, activo = TRUE, fecha_actualizacion = CURRENT_TIMESTAMP
WHERE codigo = 'DENPLA';

UPDATE endpoints_busqueda
SET metodo_proveedor = 'GET', ruta_proveedor = '/api/v1/consultas/fd/plat/{placa}', parametro_principal = 'placa',
    tipo_consumo_proveedor = 'CREDITO', costo_proveedor = 5, activo = TRUE, fecha_actualizacion = CURRENT_TIMESTAMP
WHERE codigo = 'PLAT';

UPDATE endpoints_busqueda
SET metodo_proveedor = 'GET', ruta_proveedor = '/api/v1/consultas/fd/hsoat/{placa}', parametro_principal = 'placa',
    tipo_consumo_proveedor = 'CREDITO', costo_proveedor = 8, activo = TRUE, fecha_actualizacion = CURRENT_TIMESTAMP
WHERE codigo = 'HSOAT';
