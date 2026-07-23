-- Idempotencia para recargas MiPlata y pagos: evita registros duplicados por reintentos.
ALTER TABLE solicitudes_recarga_miplata ADD COLUMN clave_idempotencia VARCHAR(80) NULL;
CREATE UNIQUE INDEX uk_solicitudes_recarga_miplata_idem ON solicitudes_recarga_miplata(clave_idempotencia);

ALTER TABLE pagos ADD COLUMN clave_idempotencia VARCHAR(80) NULL;
CREATE UNIQUE INDEX uk_pagos_idem ON pagos(clave_idempotencia);
