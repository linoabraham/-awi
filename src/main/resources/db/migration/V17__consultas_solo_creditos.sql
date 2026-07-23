-- Desacople créditos ↔ membresía: una consulta puede ser SOLO por créditos (sin
-- membresía). Se permite NULL en las FK de membresía y se agrega el costo en
-- créditos por endpoint para el cliente free (editable desde el panel).
ALTER TABLE consultas
  MODIFY COLUMN id_membresia BIGINT NULL,
  MODIFY COLUMN id_membresia_endpoint BIGINT NULL;

ALTER TABLE endpoints_busqueda
  ADD COLUMN costo_creditos_cliente INT NULL;
