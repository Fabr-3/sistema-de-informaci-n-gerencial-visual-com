-- =====================================================================
-- V5 - Registro de varias unidades de un modelo con codigos propios.
-- Cada modelo controlado por unidad define un prefijo; los codigos de
-- inventario se generan como PREFIJO-0001, PREFIJO-0002, ... El correlativo
-- se calcula desde el mayor codigo existente (no requiere tabla propia).
-- =====================================================================
ALTER TABLE modelos_activo
    ADD COLUMN prefijo_codigo VARCHAR(10),
    ADD CONSTRAINT ux_modelos_prefijo_codigo UNIQUE (prefijo_codigo),
    ADD CONSTRAINT ck_modelos_prefijo_formato CHECK (prefijo_codigo IS NULL OR prefijo_codigo ~ '^[A-Z0-9]{2,10}$'),
    ADD CONSTRAINT ck_modelos_prefijo_solo_unidad CHECK (tipo_control = 'UNIDAD' OR prefijo_codigo IS NULL);
