-- =====================================================================
-- V20 - Subalquiler de equipos: cuando un evento necesita mas unidades de
-- un modelo de las que hay libres, lo que falta se alquila a un proveedor.
-- Se registra como subcontratacion (con su egreso imputado al proyecto y al
-- evento) indicando el modelo y la cantidad subalquilada.
-- =====================================================================

ALTER TABLE subcontrataciones
    ADD COLUMN id_modelo INT REFERENCES modelos_activo(id_modelo),
    ADD COLUMN cantidad  INT CHECK (cantidad > 0),
    ADD CONSTRAINT ck_subcontrataciones_subalquiler CHECK (
        (id_modelo IS NULL AND cantidad IS NULL)
        OR (id_modelo IS NOT NULL AND cantidad IS NOT NULL AND id_evento IS NOT NULL));

CREATE INDEX ix_subcontrataciones_evento_modelo ON subcontrataciones (id_evento)
    WHERE id_modelo IS NOT NULL;
