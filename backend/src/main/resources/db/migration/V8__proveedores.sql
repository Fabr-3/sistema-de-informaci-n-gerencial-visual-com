-- =====================================================================
-- V8 - Registro basico de proveedores para el modulo financiero: razon
-- social, NIT, contacto y servicios que ofrece. La evaluacion por margen
-- de costos llega con el modulo de proveedores y consumibles.
-- =====================================================================
ALTER TABLE proveedores
    ADD COLUMN telefono VARCHAR(30),
    ADD COLUMN email    VARCHAR(150);

CREATE INDEX ix_proveedores_razon_social ON proveedores (lower(razon_social));
