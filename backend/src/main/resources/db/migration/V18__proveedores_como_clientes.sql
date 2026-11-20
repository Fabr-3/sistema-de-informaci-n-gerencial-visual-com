-- =====================================================================
-- V18 - Datos del proveedor como los del cliente.
--   * Tipo de proveedor (persona natural, empresa privada o institucion
--     publica), direccion y varios documentos (con complemento del carnet).
--   * Estado activo: un proveedor anulado conserva su historial, pero no se
--     elige en movimientos ni subcontrataciones nuevos.
-- =====================================================================

CREATE TABLE tipos_proveedor (
    id_tipo_proveedor  SERIAL PRIMARY KEY,
    nombre             VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO tipos_proveedor (nombre) VALUES
    ('Persona natural'),
    ('Empresa privada'),
    ('Institución pública');

ALTER TABLE proveedores
    ADD COLUMN id_tipo_proveedor INT REFERENCES tipos_proveedor(id_tipo_proveedor),
    ADD COLUMN direccion TEXT,
    ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;

-- Los proveedores registrados hasta ahora son empresas
UPDATE proveedores
SET id_tipo_proveedor = (SELECT id_tipo_proveedor FROM tipos_proveedor WHERE nombre = 'Empresa privada');

ALTER TABLE proveedores ALTER COLUMN id_tipo_proveedor SET NOT NULL;
