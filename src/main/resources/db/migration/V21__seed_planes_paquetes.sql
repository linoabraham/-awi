-- =====================================================================
-- V21 · Semilla de planes mensuales, paquetes de créditos y accesos
-- =====================================================================
-- Puebla la data comercial que V2 dejó vacía (planes y paquetes). Corre
-- automáticamente al arrancar el backend (Flyway) una sola vez.
--
-- Decisiones del usuario (jul-2026):
--  · Planes mensuales: Básico S/29.90, Pro S/59.90, Ilimitado S/99.90 (30 d)
--    + Ilimitado Quincenal S/59.90 (15 d). El nombre lleva "Ilimitado" para
--    que la app lo pinte como tarjeta premium.
--  · Paquetes de crédito: 20/S8, 50/S18, 100/S32, 250/S70, 500/S130.
--  · Precio de servicios: se mantiene 1:1 con el proveedor (costo_creditos_cliente
--    queda NULL en endpoints_busqueda → usa costo_proveedor). El margen va en el
--    precio del crédito. Los servicios nuevos C4 (6 créd) y Árbol genealógico
--    (40 créd) se fijan en el catálogo de la app, no aquí (C4 reusa el endpoint
--    DNIT y Árbol orquesta varias llamadas del lado cliente).
--
-- Reglas de acceso por plan (planes_endpoints): requiere_finalidad y
-- requiere_justificacion se toman de es_critico (los sensibles siempre las
-- piden). MFA opcional (false). Guardas NOT EXISTS por si el admin ya creó
-- algún plan/paquete con el mismo código.
-- =====================================================================

-- ---------- PLANES (membresías) ----------
INSERT INTO planes (codigo, nombre, descripcion, precio_soles, dias_vigencia, estado, fecha_creacion, fecha_actualizacion)
SELECT * FROM (
  SELECT 'PLAN_BASICO_MENSUAL'      AS codigo, 'Plan Básico'               AS nombre, 'Identidad y placas básicas incluidas con tope diario; el resto por créditos con acceso completo.' AS descripcion, 29.90 AS precio_soles, 30 AS dias_vigencia, 'ACTIVO' AS estado, NOW() AS fc, NOW() AS fa
  UNION ALL SELECT 'PLAN_PRO_MENSUAL',        'Plan Pro',                 'Identidad completa (fotos, huellas, familiares) y vehículos incluidos; denuncias/teléfonos/facial por créditos.', 59.90, 30, 'ACTIVO', NOW(), NOW()
  UNION ALL SELECT 'PLAN_ILIMITADO_MENSUAL',  'Plan Ilimitado',           'Todos los servicios sin tope durante 30 días, incluido el árbol genealógico.', 99.90, 30, 'ACTIVO', NOW(), NOW()
  UNION ALL SELECT 'PLAN_ILIMITADO_QUINCENAL','Plan Ilimitado Quincenal', 'Todos los servicios sin tope durante 15 días, incluido el árbol genealógico.', 59.90, 15, 'ACTIVO', NOW(), NOW()
) AS nuevos
WHERE NOT EXISTS (SELECT 1 FROM planes p WHERE p.codigo = nuevos.codigo);

-- ---------- PAQUETES DE CRÉDITOS ----------
INSERT INTO paquetes_creditos (codigo, nombre, cantidad_creditos, precio_soles, dias_vigencia, activo, fecha_creacion, fecha_actualizacion)
SELECT * FROM (
  SELECT 'PAQ_20_CREDITOS'  AS codigo, '20 créditos'  AS nombre,  20 AS cantidad_creditos,   8.00 AS precio_soles, 180 AS dias_vigencia, TRUE AS activo, NOW() AS fc, NOW() AS fa
  UNION ALL SELECT 'PAQ_50_CREDITOS',  '50 créditos',   50,  18.00, 180, TRUE, NOW(), NOW()
  UNION ALL SELECT 'PAQ_100_CREDITOS', '100 créditos', 100,  32.00, 240, TRUE, NOW(), NOW()
  UNION ALL SELECT 'PAQ_250_CREDITOS', '250 créditos', 250,  70.00, 365, TRUE, NOW(), NOW()
  UNION ALL SELECT 'PAQ_500_CREDITOS', '500 créditos', 500, 130.00, 365, TRUE, NOW(), NOW()
) AS nuevos
WHERE NOT EXISTS (SELECT 1 FROM paquetes_creditos pc WHERE pc.codigo = nuevos.codigo);

-- =====================================================================
-- ACCESOS POR PLAN (planes_endpoints)
-- Cada plan referencia sus endpoints por código (robusto ante los IDs).
-- =====================================================================

-- ---------- PLAN BÁSICO ----------
-- Incluidos con tope diario 20: identidad ligera + placa + RUC.
INSERT INTO planes_endpoints
  (id_plan, id_endpoint, habilitado, modalidad_acceso, limite_diario, limite_ciclo, costo_creditos_cliente,
   requiere_mfa, requiere_finalidad, requiere_justificacion, permite_exportar, dias_retencion, fecha_creacion, fecha_actualizacion)
SELECT p.id_plan, e.id_endpoint, TRUE, 'INCLUIDO_MEMBRESIA', 20, NULL, NULL,
       FALSE, e.es_critico, e.es_critico, TRUE, 30, NOW(), NOW()
FROM planes p JOIN endpoints_busqueda e
WHERE p.codigo = 'PLAN_BASICO_MENSUAL'
  AND e.codigo IN ('DNI_BASICO','DNI_COMPLETO','RUC','PLA')
  AND NOT EXISTS (SELECT 1 FROM planes_endpoints pe WHERE pe.id_plan = p.id_plan AND pe.id_endpoint = e.id_endpoint);

-- El resto de servicios: por créditos, al precio estándar (= costo del proveedor).
INSERT INTO planes_endpoints
  (id_plan, id_endpoint, habilitado, modalidad_acceso, limite_diario, limite_ciclo, costo_creditos_cliente,
   requiere_mfa, requiere_finalidad, requiere_justificacion, permite_exportar, dias_retencion, fecha_creacion, fecha_actualizacion)
SELECT p.id_plan, e.id_endpoint, TRUE, 'DESCUENTO_CREDITOS', NULL, NULL, COALESCE(e.costo_creditos_cliente, e.costo_proveedor),
       FALSE, e.es_critico, e.es_critico, TRUE, 30, NOW(), NOW()
FROM planes p JOIN endpoints_busqueda e
WHERE p.codigo = 'PLAN_BASICO_MENSUAL'
  AND e.codigo NOT IN ('DNI_BASICO','DNI_COMPLETO','RUC','PLA')
  AND NOT EXISTS (SELECT 1 FROM planes_endpoints pe WHERE pe.id_plan = p.id_plan AND pe.id_endpoint = e.id_endpoint);

-- ---------- PLAN PRO ----------
-- Incluidos con tope diario 50: identidad completa + vehículos.
INSERT INTO planes_endpoints
  (id_plan, id_endpoint, habilitado, modalidad_acceso, limite_diario, limite_ciclo, costo_creditos_cliente,
   requiere_mfa, requiere_finalidad, requiere_justificacion, permite_exportar, dias_retencion, fecha_creacion, fecha_actualizacion)
SELECT p.id_plan, e.id_endpoint, TRUE, 'INCLUIDO_MEMBRESIA', 50, NULL, NULL,
       FALSE, e.es_critico, e.es_critico, TRUE, 45, NOW(), NOW()
FROM planes p JOIN endpoints_busqueda e
WHERE p.codigo = 'PLAN_PRO_MENSUAL'
  AND e.codigo IN ('DNI_BASICO','DNI_COMPLETO','DNIV','DNIVEL','DNIT','NM','AG','RUC','PLA','PLAT','HSOAT')
  AND NOT EXISTS (SELECT 1 FROM planes_endpoints pe WHERE pe.id_plan = p.id_plan AND pe.id_endpoint = e.id_endpoint);

-- Sensibles (denuncias, teléfonos, facial, requisitorias): por créditos.
INSERT INTO planes_endpoints
  (id_plan, id_endpoint, habilitado, modalidad_acceso, limite_diario, limite_ciclo, costo_creditos_cliente,
   requiere_mfa, requiere_finalidad, requiere_justificacion, permite_exportar, dias_retencion, fecha_creacion, fecha_actualizacion)
SELECT p.id_plan, e.id_endpoint, TRUE, 'DESCUENTO_CREDITOS', NULL, NULL, COALESCE(e.costo_creditos_cliente, e.costo_proveedor),
       FALSE, e.es_critico, e.es_critico, TRUE, 45, NOW(), NOW()
FROM planes p JOIN endpoints_busqueda e
WHERE p.codigo = 'PLAN_PRO_MENSUAL'
  AND e.codigo IN ('TELP_DNI','TELP_CEL','DEN','DENUNCIAS','RQH','FACIAL_TOP','DENPLA')
  AND NOT EXISTS (SELECT 1 FROM planes_endpoints pe WHERE pe.id_plan = p.id_plan AND pe.id_endpoint = e.id_endpoint);

-- ---------- PLANES ILIMITADOS (mensual + quincenal) ----------
-- TODOS los servicios incluidos y sin tope (limite_diario/ciclo NULL).
INSERT INTO planes_endpoints
  (id_plan, id_endpoint, habilitado, modalidad_acceso, limite_diario, limite_ciclo, costo_creditos_cliente,
   requiere_mfa, requiere_finalidad, requiere_justificacion, permite_exportar, dias_retencion, fecha_creacion, fecha_actualizacion)
SELECT p.id_plan, e.id_endpoint, TRUE, 'INCLUIDO_MEMBRESIA', NULL, NULL, NULL,
       FALSE, e.es_critico, e.es_critico, TRUE, 90, NOW(), NOW()
FROM planes p JOIN endpoints_busqueda e
WHERE p.codigo IN ('PLAN_ILIMITADO_MENSUAL','PLAN_ILIMITADO_QUINCENAL')
  AND NOT EXISTS (SELECT 1 FROM planes_endpoints pe WHERE pe.id_plan = p.id_plan AND pe.id_endpoint = e.id_endpoint);
