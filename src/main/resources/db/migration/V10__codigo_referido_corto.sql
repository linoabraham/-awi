-- Reformatea los códigos de referido existentes al nuevo formato corto de
-- 5 caracteres (letras y números, sin Ñ). Deriva un valor aleatorio por fila
-- a partir de un hash; el índice UNIQUE garantiza que no se repitan.
-- Válido en etapa temprana: aún no hay códigos compartidos por usuarios reales.
UPDATE clientes
SET codigo_referido = UPPER(SUBSTRING(MD5(CONCAT(id_cliente, '-', UUID())), 1, 5));
