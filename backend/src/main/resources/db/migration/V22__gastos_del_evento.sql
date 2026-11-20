-- =====================================================================
-- V22 - Gastos del evento: compras o pagos menores que el personal tecnico
-- hace durante un evento y que no estan en el catalogo de consumibles
-- (por ejemplo, un repuesto o un pasaje). Cada gasto lleva su comprobante
-- (foto o PDF) y registra en la misma operacion un egreso de sistema
-- imputado al proyecto y al evento. No mueve stock.
-- =====================================================================

INSERT INTO tipos_movimiento (nombre, naturaleza, requiere_proveedor, descuenta_inventario, codigo_sistema)
VALUES ('Gastos del evento', 'EGRESO', FALSE, FALSE, 'GASTO_EVENTO')
ON CONFLICT (nombre) DO UPDATE SET codigo_sistema = EXCLUDED.codigo_sistema,
                                   requiere_proveedor = FALSE, descuenta_inventario = FALSE;

CREATE TABLE gastos_evento (
    id_gasto              SERIAL PRIMARY KEY,
    id_proyecto           INT NOT NULL REFERENCES proyectos(id_proyecto),
    id_evento             INT NOT NULL REFERENCES eventos(id_evento),
    descripcion           VARCHAR(500) NOT NULL,
    monto                 NUMERIC(12,2) NOT NULL CHECK (monto > 0),
    fecha                 DATE NOT NULL,
    comprobante_archivo   VARCHAR(80) NOT NULL UNIQUE,
    comprobante_nombre    VARCHAR(255) NOT NULL,
    comprobante_tipo      VARCHAR(50) NOT NULL
        CHECK (comprobante_tipo IN ('application/pdf', 'image/jpeg', 'image/png', 'image/webp')),
    estado                VARCHAR(15) NOT NULL DEFAULT 'REGISTRADO' CHECK (estado IN ('REGISTRADO', 'ANULADO')),
    motivo_anulacion      TEXT,
    id_movimiento         INT UNIQUE REFERENCES movimientos_financieros(id_movimiento),
    registrado_por        INT REFERENCES usuarios(id_usuario),
    modificado_por        INT REFERENCES usuarios(id_usuario),
    fecha_creacion        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion    TIMESTAMP,
    CONSTRAINT ck_gastos_evento_anulacion CHECK (estado <> 'ANULADO' OR motivo_anulacion IS NOT NULL)
);
CREATE INDEX ix_gastos_evento_evento ON gastos_evento (id_evento, fecha DESC);

-- La funcion de V9 solo usa id_evento e id_proyecto
CREATE TRIGGER trg_gasto_evento_del_proyecto
BEFORE INSERT OR UPDATE OF id_proyecto, id_evento ON gastos_evento
FOR EACH ROW EXECUTE FUNCTION fn_movimiento_evento_del_proyecto();
