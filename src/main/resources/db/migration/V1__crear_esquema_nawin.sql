CREATE TABLE roles (
                       id_rol BIGINT AUTO_INCREMENT PRIMARY KEY,
                       codigo VARCHAR(20) NOT NULL,
                       nombre VARCHAR(50) NOT NULL,
                       activo BOOLEAN NOT NULL DEFAULT TRUE,
                       CONSTRAINT uk_roles_codigo UNIQUE (codigo)
);

CREATE TABLE usuarios (
                          id_usuario BIGINT AUTO_INCREMENT PRIMARY KEY,
                          id_rol BIGINT NOT NULL,
                          nombres VARCHAR(100) NOT NULL,
                          apellidos VARCHAR(120) NOT NULL,
                          correo VARCHAR(150) NOT NULL,
                          celular VARCHAR(15) NOT NULL,
                          nombre_usuario VARCHAR(60) NOT NULL,
                          clave_hash VARCHAR(255) NOT NULL,
                          estado VARCHAR(20) NOT NULL,
                          mfa_habilitado BOOLEAN NOT NULL DEFAULT FALSE,
                          tipo_mfa VARCHAR(20) NULL,
                          mfa_secreto_cifrado VARCHAR(500) NULL,
                          intentos_fallidos INT NOT NULL DEFAULT 0,
                          bloqueado_hasta DATETIME NULL,
                          ultimo_acceso DATETIME NULL,
                          fecha_creacion DATETIME NOT NULL,
                          fecha_actualizacion DATETIME NOT NULL,
                          CONSTRAINT uk_usuarios_correo UNIQUE (correo),
                          CONSTRAINT uk_usuarios_nombre_usuario UNIQUE (nombre_usuario),
                          CONSTRAINT fk_usuarios_roles FOREIGN KEY (id_rol) REFERENCES roles(id_rol)
);
CREATE INDEX idx_usuarios_rol ON usuarios(id_rol);
CREATE INDEX idx_usuarios_estado ON usuarios(estado);

CREATE TABLE clientes (
                          id_cliente BIGINT AUTO_INCREMENT PRIMARY KEY,
                          id_usuario BIGINT NOT NULL,
                          tipo_documento VARCHAR(10) NOT NULL,
                          numero_documento VARCHAR(11) NOT NULL,
                          nombres VARCHAR(100) NULL,
                          apellidos VARCHAR(120) NULL,
                          razon_social VARCHAR(200) NULL,
                          direccion VARCHAR(250) NULL,
                          estado VARCHAR(20) NOT NULL,
                          creado_por BIGINT NOT NULL,
                          fecha_creacion DATETIME NOT NULL,
                          fecha_actualizacion DATETIME NOT NULL,
                          CONSTRAINT uk_clientes_usuario UNIQUE (id_usuario),
                          CONSTRAINT uk_clientes_documento UNIQUE (numero_documento),
                          CONSTRAINT fk_clientes_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
                          CONSTRAINT fk_clientes_creador FOREIGN KEY (creado_por) REFERENCES usuarios(id_usuario)
);
CREATE INDEX idx_clientes_estado ON clientes(estado);
CREATE INDEX idx_clientes_creador ON clientes(creado_por);

CREATE TABLE planes (
                        id_plan BIGINT AUTO_INCREMENT PRIMARY KEY,
                        codigo VARCHAR(30) NOT NULL,
                        nombre VARCHAR(100) NOT NULL,
                        descripcion VARCHAR(500) NULL,
                        precio_soles DECIMAL(12,2) NOT NULL,
                        dias_vigencia INT NOT NULL,
                        estado VARCHAR(20) NOT NULL,
                        fecha_creacion DATETIME NOT NULL,
                        fecha_actualizacion DATETIME NOT NULL,
                        CONSTRAINT uk_planes_codigo UNIQUE (codigo)
);
CREATE INDEX idx_planes_estado ON planes(estado);

CREATE TABLE endpoints_busqueda (
                                    id_endpoint BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    codigo VARCHAR(40) NOT NULL,
                                    nombre VARCHAR(120) NOT NULL,
                                    descripcion VARCHAR(500) NULL,
                                    metodo_proveedor VARCHAR(10) NOT NULL,
                                    ruta_proveedor VARCHAR(300) NOT NULL,
                                    parametro_principal VARCHAR(50) NOT NULL,
                                    tipo_consumo_proveedor VARCHAR(20) NOT NULL,
                                    costo_proveedor INT NOT NULL,
                                    es_critico BOOLEAN NOT NULL,
                                    activo BOOLEAN NOT NULL,
                                    fecha_creacion DATETIME NOT NULL,
                                    fecha_actualizacion DATETIME NOT NULL,
                                    CONSTRAINT uk_endpoints_codigo UNIQUE (codigo)
);
CREATE INDEX idx_endpoints_activo ON endpoints_busqueda(activo);
CREATE INDEX idx_endpoints_critico ON endpoints_busqueda(es_critico);

CREATE TABLE planes_endpoints (
                                  id_plan_endpoint BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  id_plan BIGINT NOT NULL,
                                  id_endpoint BIGINT NOT NULL,
                                  habilitado BOOLEAN NOT NULL,
                                  modalidad_acceso VARCHAR(30) NOT NULL,
                                  limite_diario INT NULL,
                                  limite_ciclo INT NULL,
                                  costo_creditos_cliente INT NULL,
                                  requiere_mfa BOOLEAN NOT NULL,
                                  requiere_finalidad BOOLEAN NOT NULL,
                                  requiere_justificacion BOOLEAN NOT NULL,
                                  permite_exportar BOOLEAN NOT NULL,
                                  dias_retencion INT NOT NULL,
                                  fecha_creacion DATETIME NOT NULL,
                                  fecha_actualizacion DATETIME NOT NULL,
                                  CONSTRAINT uk_planes_endpoints_plan_endpoint UNIQUE (id_plan, id_endpoint),
                                  CONSTRAINT fk_planes_endpoints_plan FOREIGN KEY (id_plan) REFERENCES planes(id_plan),
                                  CONSTRAINT fk_planes_endpoints_endpoint FOREIGN KEY (id_endpoint) REFERENCES endpoints_busqueda(id_endpoint)
);
CREATE INDEX idx_planes_endpoints_plan ON planes_endpoints(id_plan);
CREATE INDEX idx_planes_endpoints_endpoint ON planes_endpoints(id_endpoint);

CREATE TABLE membresias (
                            id_membresia BIGINT AUTO_INCREMENT PRIMARY KEY,
                            id_cliente BIGINT NOT NULL,
                            id_plan BIGINT NOT NULL,
                            id_membresia_anterior BIGINT NULL,
                            fecha_inicio DATE NOT NULL,
                            fecha_fin DATE NOT NULL,
                            dias_vigencia INT NOT NULL,
                            precio_pagado DECIMAL(12,2) NOT NULL,
                            estado VARCHAR(20) NOT NULL,
                            observacion VARCHAR(500) NULL,
                            creado_por BIGINT NOT NULL,
                            fecha_creacion DATETIME NOT NULL,
                            fecha_activacion DATETIME NULL,
                            fecha_actualizacion DATETIME NOT NULL,
                            CONSTRAINT fk_membresias_cliente FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente),
                            CONSTRAINT fk_membresias_plan FOREIGN KEY (id_plan) REFERENCES planes(id_plan),
                            CONSTRAINT fk_membresias_anterior FOREIGN KEY (id_membresia_anterior) REFERENCES membresias(id_membresia),
                            CONSTRAINT fk_membresias_creador FOREIGN KEY (creado_por) REFERENCES usuarios(id_usuario)
);
CREATE INDEX idx_membresias_cliente_estado ON membresias(id_cliente, estado);
CREATE INDEX idx_membresias_fechas ON membresias(fecha_inicio, fecha_fin);

CREATE TABLE membresias_endpoints (
                                      id_membresia_endpoint BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      id_membresia BIGINT NOT NULL,
                                      id_endpoint BIGINT NOT NULL,
                                      habilitado BOOLEAN NOT NULL,
                                      modalidad_acceso VARCHAR(30) NOT NULL,
                                      limite_diario INT NULL,
                                      limite_total INT NULL,
                                      consumido_total INT NOT NULL DEFAULT 0,
                                      costo_creditos_cliente INT NULL,
                                      requiere_mfa BOOLEAN NOT NULL,
                                      requiere_finalidad BOOLEAN NOT NULL,
                                      requiere_justificacion BOOLEAN NOT NULL,
                                      permite_exportar BOOLEAN NOT NULL,
                                      dias_retencion INT NOT NULL,
                                      fecha_creacion DATETIME NOT NULL,
                                      fecha_actualizacion DATETIME NOT NULL,
                                      CONSTRAINT uk_membresias_endpoints_membresia_endpoint UNIQUE (id_membresia, id_endpoint),
                                      CONSTRAINT fk_membresias_endpoints_membresia FOREIGN KEY (id_membresia) REFERENCES membresias(id_membresia),
                                      CONSTRAINT fk_membresias_endpoints_endpoint FOREIGN KEY (id_endpoint) REFERENCES endpoints_busqueda(id_endpoint)
);
CREATE INDEX idx_membresias_endpoints_membresia ON membresias_endpoints(id_membresia);
CREATE INDEX idx_membresias_endpoints_endpoint ON membresias_endpoints(id_endpoint);

CREATE TABLE paquetes_creditos (
                                   id_paquete_credito BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   codigo VARCHAR(30) NOT NULL,
                                   nombre VARCHAR(100) NOT NULL,
                                   cantidad_creditos INT NOT NULL,
                                   precio_soles DECIMAL(12,2) NOT NULL,
                                   dias_vigencia INT NOT NULL,
                                   activo BOOLEAN NOT NULL,
                                   fecha_creacion DATETIME NOT NULL,
                                   fecha_actualizacion DATETIME NOT NULL,
                                   CONSTRAINT uk_paquetes_creditos_codigo UNIQUE (codigo)
);
CREATE INDEX idx_paquetes_creditos_activo ON paquetes_creditos(activo);

CREATE TABLE ventas_creditos (
                                 id_venta_credito BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 id_cliente BIGINT NOT NULL,
                                 id_paquete_credito BIGINT NOT NULL,
                                 cantidad_paquetes INT NOT NULL,
                                 creditos_otorgados INT NOT NULL,
                                 precio_unitario DECIMAL(12,2) NOT NULL,
                                 total_soles DECIMAL(12,2) NOT NULL,
                                 fecha_vencimiento_creditos DATE NOT NULL,
                                 estado VARCHAR(20) NOT NULL,
                                 creado_por BIGINT NOT NULL,
                                 fecha_creacion DATETIME NOT NULL,
                                 fecha_actualizacion DATETIME NOT NULL,
                                 CONSTRAINT fk_ventas_creditos_cliente FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente),
                                 CONSTRAINT fk_ventas_creditos_paquete FOREIGN KEY (id_paquete_credito) REFERENCES paquetes_creditos(id_paquete_credito),
                                 CONSTRAINT fk_ventas_creditos_creador FOREIGN KEY (creado_por) REFERENCES usuarios(id_usuario)
);
CREATE INDEX idx_ventas_creditos_cliente ON ventas_creditos(id_cliente);
CREATE INDEX idx_ventas_creditos_estado ON ventas_creditos(estado);

CREATE TABLE saldos_creditos (
                                 id_saldo_credito BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 id_cliente BIGINT NOT NULL,
                                 creditos_disponibles INT NOT NULL DEFAULT 0,
                                 creditos_reservados INT NOT NULL DEFAULT 0,
                                 creditos_consumidos INT NOT NULL DEFAULT 0,
                                 version BIGINT NOT NULL DEFAULT 0,
                                 fecha_actualizacion DATETIME NOT NULL,
                                 CONSTRAINT uk_saldos_creditos_cliente UNIQUE (id_cliente),
                                 CONSTRAINT fk_saldos_creditos_cliente FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente)
);

CREATE TABLE pagos (
                       id_pago BIGINT AUTO_INCREMENT PRIMARY KEY,
                       id_cliente BIGINT NOT NULL,
                       id_membresia BIGINT NULL,
                       id_venta_credito BIGINT NULL,
                       monto_soles DECIMAL(12,2) NOT NULL,
                       medio_pago VARCHAR(30) NOT NULL,
                       numero_operacion VARCHAR(100) NULL,
                       fecha_pago DATETIME NOT NULL,
                       estado VARCHAR(20) NOT NULL,
                       observacion VARCHAR(500) NULL,
                       registrado_por BIGINT NOT NULL,
                       fecha_creacion DATETIME NOT NULL,
                       fecha_actualizacion DATETIME NOT NULL,
                       CONSTRAINT fk_pagos_cliente FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente),
                       CONSTRAINT fk_pagos_membresia FOREIGN KEY (id_membresia) REFERENCES membresias(id_membresia),
                       CONSTRAINT fk_pagos_venta_credito FOREIGN KEY (id_venta_credito) REFERENCES ventas_creditos(id_venta_credito),
                       CONSTRAINT fk_pagos_registrador FOREIGN KEY (registrado_por) REFERENCES usuarios(id_usuario)
);
CREATE INDEX idx_pagos_cliente ON pagos(id_cliente);
CREATE INDEX idx_pagos_estado ON pagos(estado);

CREATE TABLE series_comprobantes (
                                     id_serie_comprobante BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     tipo_comprobante VARCHAR(30) NOT NULL,
                                     serie VARCHAR(10) NOT NULL,
                                     ultimo_numero BIGINT NOT NULL,
                                     activo BOOLEAN NOT NULL,
                                     fecha_creacion DATETIME NOT NULL,
                                     fecha_actualizacion DATETIME NOT NULL,
                                     CONSTRAINT uk_series_comprobantes_serie UNIQUE (serie)
);
CREATE INDEX idx_series_comprobantes_tipo ON series_comprobantes(tipo_comprobante);

CREATE TABLE comprobantes (
                              id_comprobante BIGINT AUTO_INCREMENT PRIMARY KEY,
                              id_pago BIGINT NOT NULL,
                              id_serie_comprobante BIGINT NOT NULL,
                              tipo_comprobante VARCHAR(30) NOT NULL,
                              serie VARCHAR(10) NOT NULL,
                              numero BIGINT NOT NULL,
                              tipo_documento_cliente VARCHAR(10) NOT NULL,
                              numero_documento_cliente VARCHAR(11) NOT NULL,
                              nombre_cliente VARCHAR(200) NOT NULL,
                              subtotal DECIMAL(12,2) NOT NULL,
                              igv DECIMAL(12,2) NOT NULL,
                              total DECIMAL(12,2) NOT NULL,
                              estado VARCHAR(20) NOT NULL,
                              ruta_pdf VARCHAR(500) NOT NULL,
                              emitido_por BIGINT NOT NULL,
                              fecha_emision DATETIME NOT NULL,
                              fecha_anulacion DATETIME NULL,
                              motivo_anulacion VARCHAR(300) NULL,
                              CONSTRAINT uk_comprobantes_pago UNIQUE (id_pago),
                              CONSTRAINT uk_comprobantes_serie_numero UNIQUE (serie, numero),
                              CONSTRAINT fk_comprobantes_pago FOREIGN KEY (id_pago) REFERENCES pagos(id_pago),
                              CONSTRAINT fk_comprobantes_serie FOREIGN KEY (id_serie_comprobante) REFERENCES series_comprobantes(id_serie_comprobante),
                              CONSTRAINT fk_comprobantes_emisor FOREIGN KEY (emitido_por) REFERENCES usuarios(id_usuario)
);

-- CORRECCIÓN: codigo_consulta ahora es VARCHAR(36)
CREATE TABLE consultas (
                           id_consulta BIGINT AUTO_INCREMENT PRIMARY KEY,
                           codigo_consulta VARCHAR(36) NOT NULL,
                           id_cliente BIGINT NOT NULL,
                           id_usuario BIGINT NOT NULL,
                           id_membresia BIGINT NOT NULL,
                           id_membresia_endpoint BIGINT NOT NULL,
                           id_endpoint BIGINT NOT NULL,
                           parametro_cifrado LONGTEXT NOT NULL,
                           parametro_mascara VARCHAR(100) NOT NULL,
                           finalidad VARCHAR(200) NULL,
                           justificacion VARCHAR(500) NULL,
                           origen_consumo VARCHAR(20) NOT NULL,
                           cantidad_consumida INT NOT NULL,
                           estado VARCHAR(30) NOT NULL,
                           codigo_http_proveedor INT NULL,
                           duracion_milisegundos BIGINT NULL,
                           mensaje_error VARCHAR(500) NULL,
                           direccion_ip VARCHAR(45) NOT NULL,
                           agente_usuario VARCHAR(500) NULL,
                           visible_cliente BOOLEAN NOT NULL DEFAULT TRUE,
                           fecha_inicio DATETIME NOT NULL,
                           fecha_fin DATETIME NULL,
                           clave_idempotencia VARCHAR(120) NULL,
                           CONSTRAINT uk_consultas_codigo UNIQUE (codigo_consulta),
                           CONSTRAINT uk_consultas_usuario_idempotencia UNIQUE (id_usuario, clave_idempotencia),
                           CONSTRAINT fk_consultas_cliente FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente),
                           CONSTRAINT fk_consultas_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
                           CONSTRAINT fk_consultas_membresia FOREIGN KEY (id_membresia) REFERENCES membresias(id_membresia),
                           CONSTRAINT fk_consultas_membresia_endpoint FOREIGN KEY (id_membresia_endpoint) REFERENCES membresias_endpoints(id_membresia_endpoint),
                           CONSTRAINT fk_consultas_endpoint FOREIGN KEY (id_endpoint) REFERENCES endpoints_busqueda(id_endpoint)
);
CREATE INDEX idx_consultas_cliente_fecha ON consultas(id_cliente, fecha_inicio);
CREATE INDEX idx_consultas_endpoint_estado ON consultas(id_endpoint, estado);

CREATE TABLE resultados_consulta (
                                     id_resultado_consulta BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     id_consulta BIGINT NOT NULL,
                                     resultado_json_cifrado LONGTEXT NOT NULL,
                                     resumen_json LONGTEXT NULL,
                                     fecha_expiracion DATETIME NOT NULL,
                                     eliminado_por_retencion BOOLEAN NOT NULL DEFAULT FALSE,
                                     fecha_creacion DATETIME NOT NULL,
                                     CONSTRAINT uk_resultados_consulta UNIQUE (id_consulta),
                                     CONSTRAINT fk_resultados_consulta FOREIGN KEY (id_consulta) REFERENCES consultas(id_consulta)
);
CREATE INDEX idx_resultados_expiracion ON resultados_consulta(fecha_expiracion, eliminado_por_retencion);

CREATE TABLE archivos_consulta (
                                   id_archivo_consulta BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   id_consulta BIGINT NOT NULL,
                                   tipo_archivo VARCHAR(20) NOT NULL,
                                   nombre_archivo VARCHAR(255) NOT NULL,
                                   tipo_mime VARCHAR(100) NOT NULL,
                                   ruta_privada VARCHAR(500) NOT NULL,
                                   tamano_bytes BIGINT NOT NULL,
                                   fecha_expiracion DATETIME NOT NULL,
                                   eliminado_por_retencion BOOLEAN NOT NULL DEFAULT FALSE,
                                   fecha_creacion DATETIME NOT NULL,
                                   CONSTRAINT fk_archivos_consulta FOREIGN KEY (id_consulta) REFERENCES consultas(id_consulta)
);
CREATE INDEX idx_archivos_consulta ON archivos_consulta(id_consulta);
CREATE INDEX idx_archivos_expiracion ON archivos_consulta(fecha_expiracion, eliminado_por_retencion);

CREATE TABLE movimientos_creditos (
                                      id_movimiento_credito BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      id_cliente BIGINT NOT NULL,
                                      id_venta_credito BIGINT NULL,
                                      id_consulta BIGINT NULL,
                                      tipo_movimiento VARCHAR(20) NOT NULL,
                                      cantidad INT NOT NULL,
                                      saldo_anterior INT NOT NULL,
                                      saldo_posterior INT NOT NULL,
                                      descripcion VARCHAR(300) NOT NULL,
                                      registrado_por BIGINT NULL,
                                      fecha_creacion DATETIME NOT NULL,
                                      CONSTRAINT fk_movimientos_creditos_cliente FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente),
                                      CONSTRAINT fk_movimientos_creditos_venta FOREIGN KEY (id_venta_credito) REFERENCES ventas_creditos(id_venta_credito),
                                      CONSTRAINT fk_movimientos_creditos_consulta FOREIGN KEY (id_consulta) REFERENCES consultas(id_consulta),
                                      CONSTRAINT fk_movimientos_creditos_registrador FOREIGN KEY (registrado_por) REFERENCES usuarios(id_usuario)
);
CREATE INDEX idx_movimientos_creditos_cliente ON movimientos_creditos(id_cliente, fecha_creacion);

CREATE TABLE tokens_refresco (
                                 id_token_refresco BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 id_usuario BIGINT NOT NULL,
                                 token_hash VARCHAR(255) NOT NULL,
                                 fecha_expiracion DATETIME NOT NULL,
                                 revocado BOOLEAN NOT NULL DEFAULT FALSE,
                                 direccion_ip VARCHAR(45) NOT NULL,
                                 agente_usuario VARCHAR(500) NULL,
                                 fecha_creacion DATETIME NOT NULL,
                                 CONSTRAINT uk_tokens_refresco_hash UNIQUE (token_hash),
                                 CONSTRAINT fk_tokens_refresco_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);
CREATE INDEX idx_tokens_refresco_usuario ON tokens_refresco(id_usuario, revocado);

CREATE TABLE notificaciones (
                                id_notificacion BIGINT AUTO_INCREMENT PRIMARY KEY,
                                id_cliente BIGINT NOT NULL,
                                id_membresia BIGINT NULL,
                                canal VARCHAR(20) NOT NULL,
                                tipo VARCHAR(30) NOT NULL,
                                titulo VARCHAR(150) NOT NULL,
                                mensaje VARCHAR(1000) NOT NULL,
                                estado VARCHAR(20) NOT NULL,
                                fecha_programada DATETIME NULL,
                                fecha_envio DATETIME NULL,
                                creado_por BIGINT NULL,
                                fecha_creacion DATETIME NOT NULL,
                                CONSTRAINT fk_notificaciones_cliente FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente),
                                CONSTRAINT fk_notificaciones_membresia FOREIGN KEY (id_membresia) REFERENCES membresias(id_membresia),
                                CONSTRAINT fk_notificaciones_creador FOREIGN KEY (creado_por) REFERENCES usuarios(id_usuario)
);
CREATE INDEX idx_notificaciones_cliente ON notificaciones(id_cliente, fecha_creacion);
CREATE INDEX idx_notificaciones_estado ON notificaciones(estado);

CREATE TABLE auditorias (
                            id_auditoria BIGINT AUTO_INCREMENT PRIMARY KEY,
                            id_usuario BIGINT NULL,
                            accion VARCHAR(100) NOT NULL,
                            entidad VARCHAR(100) NOT NULL,
                            id_entidad VARCHAR(100) NULL,
                            detalle_json LONGTEXT NULL,
                            direccion_ip VARCHAR(45) NULL,
                            fecha_creacion DATETIME NOT NULL,
                            CONSTRAINT fk_auditorias_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);
CREATE INDEX idx_auditorias_entidad ON auditorias(entidad);
CREATE INDEX idx_auditorias_usuario ON auditorias(id_usuario);