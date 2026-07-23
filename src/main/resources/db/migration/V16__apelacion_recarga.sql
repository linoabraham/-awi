-- Apelación del cliente cuando su recarga/comprobante fue rechazada: puede
-- enviar un mensaje pidiendo reconsideración. Solo aplica a recargas RECHAZADA.
ALTER TABLE solicitudes_recarga_miplata
  ADD COLUMN apelacion VARCHAR(500) NULL,
  ADD COLUMN fecha_apelacion DATETIME NULL,
  ADD COLUMN apelacion_respondida BIT NOT NULL DEFAULT 0;
