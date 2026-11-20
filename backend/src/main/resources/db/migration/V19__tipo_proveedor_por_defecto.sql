-- =====================================================================
-- V19 - Un proveedor registrado sin tipo (por ejemplo, con los scripts de
-- datos de prueba anteriores a V18) se considera empresa privada.
-- =====================================================================

CREATE OR REPLACE FUNCTION fn_tipo_proveedor_por_defecto() RETURNS INT
LANGUAGE sql STABLE AS $$
    SELECT id_tipo_proveedor FROM tipos_proveedor WHERE nombre = 'Empresa privada'
$$;

ALTER TABLE proveedores ALTER COLUMN id_tipo_proveedor SET DEFAULT fn_tipo_proveedor_por_defecto();
