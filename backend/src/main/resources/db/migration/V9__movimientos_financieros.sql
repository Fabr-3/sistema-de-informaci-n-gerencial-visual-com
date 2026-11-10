-- =====================================================================
-- V9 - Movimientos financieros: ingresos, gastos, compras, ventas y
-- alquileres.
--   * El proyecto es opcional (gastos e ingresos generales de la empresa)
--     y el movimiento puede imputarse a un evento del proyecto.
--   * Numero de factura externo opcional (la facturacion es externa).
--   * El tipo indica si exige proveedor y si descuenta inventario (ventas).
--   * El detalle de una venta registra las unidades y cantidades de lote
--     que salen; los equipos vendidos pasan al estado VENDIDO.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Tipos de movimiento
-- ---------------------------------------------------------------------
ALTER TABLE tipos_movimiento
    ADD COLUMN requiere_proveedor   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN descuenta_inventario BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT ck_tipos_movimiento_venta CHECK (NOT descuenta_inventario OR naturaleza = 'INGRESO'),
    ADD CONSTRAINT ck_tipos_movimiento_proveedor CHECK (NOT requiere_proveedor OR naturaleza = 'EGRESO');

INSERT INTO tipos_movimiento (nombre, naturaleza, requiere_proveedor, descuenta_inventario) VALUES
    ('Cobro de servicios',             'INGRESO', FALSE, FALSE),
    ('Alquiler de equipos',            'INGRESO', FALSE, FALSE),
    ('Venta de equipos',               'INGRESO', FALSE, TRUE),
    ('Otros ingresos',                 'INGRESO', FALSE, FALSE),
    ('Compra de equipos y materiales', 'EGRESO',  TRUE,  FALSE),
    ('Alquiler de equipos a terceros', 'EGRESO',  TRUE,  FALSE),
    ('Subcontratación de servicios',   'EGRESO',  TRUE,  FALSE),
    ('Transporte y viáticos',          'EGRESO',  FALSE, FALSE),
    ('Gastos generales',               'EGRESO',  FALSE, FALSE),
    ('Otros egresos',                  'EGRESO',  FALSE, FALSE)
ON CONFLICT (nombre) DO NOTHING;

-- ---------------------------------------------------------------------
-- Movimientos
-- ---------------------------------------------------------------------
ALTER TABLE movimientos_financieros
    ALTER COLUMN id_proyecto DROP NOT NULL,
    ADD COLUMN id_evento      INT REFERENCES eventos(id_evento),
    ADD COLUMN numero_factura VARCHAR(30),
    ADD CONSTRAINT ck_movimientos_evento_con_proyecto CHECK (id_evento IS NULL OR id_proyecto IS NOT NULL);

CREATE INDEX ix_movimientos_fecha   ON movimientos_financieros (fecha_movimiento, estado);
CREATE INDEX ix_movimientos_evento  ON movimientos_financieros (id_evento);
CREATE INDEX ix_movimientos_proveedor ON movimientos_financieros (id_proveedor);

-- El evento imputado debe pertenecer al proyecto del movimiento (tambien lo valida la aplicacion).
CREATE OR REPLACE FUNCTION fn_movimiento_evento_del_proyecto() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.id_evento IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM eventos WHERE id_evento = NEW.id_evento AND id_proyecto = NEW.id_proyecto) THEN
        RAISE EXCEPTION 'El evento % no pertenece al proyecto %', NEW.id_evento, NEW.id_proyecto
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_movimiento_evento_del_proyecto
BEFORE INSERT OR UPDATE OF id_proyecto, id_evento ON movimientos_financieros
FOR EACH ROW EXECUTE FUNCTION fn_movimiento_evento_del_proyecto();

-- ---------------------------------------------------------------------
-- Detalle de ventas: una unidad (activo) o una cantidad de un lote
-- ---------------------------------------------------------------------
CREATE TABLE ventas_detalle (
    id_detalle     SERIAL PRIMARY KEY,
    id_movimiento  INT NOT NULL REFERENCES movimientos_financieros(id_movimiento) ON DELETE CASCADE,
    id_activo      INT REFERENCES activos(id_activo),
    id_existencia  INT REFERENCES existencias_lote(id_existencia),
    cantidad       INT NOT NULL DEFAULT 1 CHECK (cantidad > 0),
    CHECK ((id_activo IS NOT NULL AND id_existencia IS NULL AND cantidad = 1)
        OR (id_activo IS NULL AND id_existencia IS NOT NULL)),
    UNIQUE (id_movimiento, id_activo),
    UNIQUE (id_movimiento, id_existencia)
);
CREATE INDEX ix_ventas_detalle_movimiento ON ventas_detalle (id_movimiento);

-- ---------------------------------------------------------------------
-- Inventario: equipos vendidos y movimientos de lote por venta
-- ---------------------------------------------------------------------
ALTER TABLE activos DROP CONSTRAINT activos_estado_operativo_check;
ALTER TABLE activos ADD CONSTRAINT activos_estado_operativo_check
    CHECK (estado_operativo IN ('DISPONIBLE', 'RESERVADO', 'EN_USO', 'EN_MANTENIMIENTO', 'BAJA', 'VENDIDO'));

ALTER TABLE movimientos_lote DROP CONSTRAINT movimientos_lote_tipo_check;
ALTER TABLE movimientos_lote ADD CONSTRAINT movimientos_lote_tipo_check
    CHECK (tipo IN ('INGRESO', 'SALIDA', 'RETORNO', 'BAJA', 'AJUSTE', 'TRASLADO_SALIDA', 'TRASLADO_ENTRADA',
                    'VENTA', 'ANULACION_VENTA'));
