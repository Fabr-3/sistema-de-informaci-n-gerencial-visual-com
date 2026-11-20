-- =====================================================================
-- V17 - Complemento del carnet de identidad.
--   * Clientes, personal tecnico y proveedores pueden registrar el
--     complemento (1 o 2 letras o numeros, por ejemplo 1A) de un carnet
--     duplicado. Un documento se distingue por tipo, numero y complemento.
-- =====================================================================

ALTER TABLE documentos_cliente
    ADD COLUMN complemento VARCHAR(2) CHECK (complemento ~ '^[0-9A-Z]{1,2}$'),
    DROP CONSTRAINT documentos_cliente_id_tipo_documento_numero_key;
CREATE UNIQUE INDEX ux_documentos_cliente_numero
    ON documentos_cliente (id_tipo_documento, upper(numero), coalesce(complemento, ''));

ALTER TABLE documentos_personal
    ADD COLUMN complemento VARCHAR(2) CHECK (complemento ~ '^[0-9A-Z]{1,2}$'),
    DROP CONSTRAINT documentos_personal_id_tipo_documento_numero_key;
CREATE UNIQUE INDEX ux_documentos_personal_numero
    ON documentos_personal (id_tipo_documento, upper(numero), coalesce(complemento, ''));

ALTER TABLE documentos_proveedor
    ADD COLUMN complemento VARCHAR(2) CHECK (complemento ~ '^[0-9A-Z]{1,2}$'),
    DROP CONSTRAINT documentos_proveedor_id_tipo_documento_numero_key;
CREATE UNIQUE INDEX ux_documentos_proveedor_numero
    ON documentos_proveedor (id_tipo_documento, upper(numero), coalesce(complemento, ''));
