-- =====================================================================
-- V14 - Modulo de subcontratacion y consumibles.
--   * Subcontrataciones de servicios a proveedores, imputadas a un proyecto
--     (y opcionalmente a un evento); cada una genera su egreso financiero.
--   * Catalogo de consumibles con stock, historial de movimientos y
--     consumos por proyecto o evento (con el costo vigente al registrar).
--   * El tipo "Subcontratacion de servicios" pasa a ser de sistema: solo lo
--     registra este modulo, para no duplicar el costo.
-- =====================================================================

INSERT INTO tipos_movimiento (nombre, naturaleza, requiere_proveedor, descuenta_inventario, codigo_sistema)
VALUES ('Subcontratación de servicios', 'EGRESO', TRUE, FALSE, 'SUBCONTRATACION')
ON CONFLICT (nombre) DO UPDATE SET codigo_sistema = EXCLUDED.codigo_sistema,
                                   requiere_proveedor = EXCLUDED.requiere_proveedor;

-- ---------------------------------------------------------------------
-- Subcontrataciones
-- ---------------------------------------------------------------------
-- servicio guarda la descripcion elegida: la lista de servicios de un
-- proveedor se reemplaza al editarlo y el vinculo puede quedar nulo.
CREATE TABLE subcontrataciones (
    id_subcontratacion  SERIAL PRIMARY KEY,
    id_proveedor        INT NOT NULL REFERENCES proveedores(id_proveedor),
    id_servicio_prov    INT REFERENCES servicios_proveedor(id_servicio_prov) ON DELETE SET NULL,
    servicio            VARCHAR(150),
    id_proyecto         INT NOT NULL REFERENCES proyectos(id_proyecto),
    id_evento           INT REFERENCES eventos(id_evento),
    descripcion         TEXT NOT NULL,
    costo               NUMERIC(12,2) NOT NULL CHECK (costo > 0),
    fecha               DATE NOT NULL,
    estado              VARCHAR(15) NOT NULL DEFAULT 'REGISTRADA' CHECK (estado IN ('REGISTRADA', 'ANULADA')),
    motivo_anulacion    TEXT,
    id_movimiento       INT UNIQUE REFERENCES movimientos_financieros(id_movimiento),
    creado_por          INT REFERENCES usuarios(id_usuario),
    modificado_por      INT REFERENCES usuarios(id_usuario),
    fecha_creacion      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion  TIMESTAMP,
    CONSTRAINT ck_subcontrataciones_anulacion CHECK (estado <> 'ANULADA' OR motivo_anulacion IS NOT NULL)
);
CREATE INDEX ix_subcontrataciones_proyecto ON subcontrataciones (id_proyecto, fecha DESC);
CREATE INDEX ix_subcontrataciones_proveedor ON subcontrataciones (id_proveedor, fecha DESC);

-- La funcion de V9 solo usa id_evento e id_proyecto: sirve para ambas tablas
CREATE TRIGGER trg_subcontratacion_evento_del_proyecto
BEFORE INSERT OR UPDATE OF id_proyecto, id_evento ON subcontrataciones
FOR EACH ROW EXECUTE FUNCTION fn_movimiento_evento_del_proyecto();

CREATE OR REPLACE FUNCTION fn_servicio_del_proveedor() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.id_servicio_prov IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM servicios_proveedor
        WHERE id_servicio_prov = NEW.id_servicio_prov AND id_proveedor = NEW.id_proveedor) THEN
        RAISE EXCEPTION 'El servicio % no pertenece al proveedor %', NEW.id_servicio_prov, NEW.id_proveedor
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_subcontratacion_servicio_del_proveedor
BEFORE INSERT OR UPDATE OF id_proveedor, id_servicio_prov ON subcontrataciones
FOR EACH ROW EXECUTE FUNCTION fn_servicio_del_proveedor();

-- ---------------------------------------------------------------------
-- Consumibles
-- ---------------------------------------------------------------------
-- stock_actual es el saldo vigente; se mantiene junto a los movimientos
-- para que el CHECK impida existencias negativas (como existencias_lote).
CREATE TABLE consumibles (
    id_consumible       SERIAL PRIMARY KEY,
    nombre              VARCHAR(120) NOT NULL UNIQUE,
    unidad_medida       VARCHAR(30) NOT NULL,
    stock_actual        NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (stock_actual >= 0),
    stock_minimo        NUMERIC(12,2) CHECK (stock_minimo IS NULL OR stock_minimo >= 0),
    costo_unitario      NUMERIC(12,2) NOT NULL CHECK (costo_unitario >= 0),
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    creado_por          INT REFERENCES usuarios(id_usuario),
    modificado_por      INT REFERENCES usuarios(id_usuario),
    fecha_creacion      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion  TIMESTAMP
);

-- cantidad lleva signo: positiva incrementa el stock, negativa lo reduce.
CREATE TABLE movimientos_consumible (
    id_movimiento   SERIAL PRIMARY KEY,
    id_consumible   INT NOT NULL REFERENCES consumibles(id_consumible) ON DELETE RESTRICT,
    tipo            VARCHAR(15) NOT NULL CHECK (tipo IN ('INGRESO', 'CONSUMO', 'AJUSTE', 'ANULACION')),
    cantidad        NUMERIC(12,2) NOT NULL CHECK (cantidad <> 0),
    fecha           TIMESTAMP NOT NULL,
    motivo          VARCHAR(255),
    creado_por      INT REFERENCES usuarios(id_usuario)
);
CREATE INDEX ix_movimientos_consumible ON movimientos_consumible (id_consumible, fecha DESC);

-- costo_unitario es una copia del costo vigente al registrar el consumo
CREATE TABLE consumos_proyecto (
    id_consumo          SERIAL PRIMARY KEY,
    id_consumible       INT NOT NULL REFERENCES consumibles(id_consumible),
    id_proyecto         INT NOT NULL REFERENCES proyectos(id_proyecto),
    id_evento           INT REFERENCES eventos(id_evento),
    cantidad            NUMERIC(12,2) NOT NULL CHECK (cantidad > 0),
    costo_unitario      NUMERIC(12,2) NOT NULL CHECK (costo_unitario >= 0),
    fecha               DATE NOT NULL,
    estado              VARCHAR(12) NOT NULL DEFAULT 'REGISTRADO' CHECK (estado IN ('REGISTRADO', 'ANULADO')),
    motivo_anulacion    TEXT,
    registrado_por      INT REFERENCES usuarios(id_usuario),
    modificado_por      INT REFERENCES usuarios(id_usuario),
    fecha_creacion      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion  TIMESTAMP,
    CONSTRAINT ck_consumos_anulacion CHECK (estado <> 'ANULADO' OR motivo_anulacion IS NOT NULL)
);
CREATE INDEX ix_consumos_proyecto ON consumos_proyecto (id_proyecto, fecha DESC);
CREATE INDEX ix_consumos_evento ON consumos_proyecto (id_evento);

CREATE TRIGGER trg_consumo_evento_del_proyecto
BEFORE INSERT OR UPDATE OF id_proyecto, id_evento ON consumos_proyecto
FOR EACH ROW EXECUTE FUNCTION fn_movimiento_evento_del_proyecto();
