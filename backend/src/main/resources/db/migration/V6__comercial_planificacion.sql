-- =====================================================================
-- V6 - Modulo comercial y de planificacion de proyectos.
--   * Cotizaciones: estados BORRADOR, ACEPTADA, RECHAZADA y VENCIDA (la
--     aprobacion la realiza el gerente), vigencia, titulo, periodo del evento
--     y datos de la resolucion.
--   * Las lineas de una cotizacion resuelta no se pueden modificar.
--   * Servicios con unidad de medida y lugares con ciudad.
--   * Catalogos iniciales de tipos de cliente, documento y contacto.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Cotizaciones
-- ---------------------------------------------------------------------
ALTER TABLE cotizaciones DROP CONSTRAINT cotizaciones_estado_check;

ALTER TABLE cotizaciones
    ADD COLUMN titulo              VARCHAR(200) NOT NULL,
    ADD COLUMN fecha_validez       DATE NOT NULL,
    ADD COLUMN fecha_evento_inicio DATE,
    ADD COLUMN fecha_evento_fin    DATE,
    ADD COLUMN resuelto_por        INT REFERENCES usuarios(id_usuario),
    ADD COLUMN fecha_resolucion    TIMESTAMP,
    ADD COLUMN motivo_rechazo      TEXT,
    ADD CONSTRAINT ck_cotizaciones_estado CHECK (estado IN ('BORRADOR', 'ACEPTADA', 'RECHAZADA', 'VENCIDA')),
    ADD CONSTRAINT ck_cotizaciones_validez CHECK (fecha_validez >= fecha_emision),
    ADD CONSTRAINT ck_cotizaciones_periodo CHECK (fecha_evento_fin IS NULL OR fecha_evento_inicio IS NULL
                                                 OR fecha_evento_fin >= fecha_evento_inicio),
    -- Aceptar o rechazar es una decision registrada; vencer la aplica el sistema
    ADD CONSTRAINT ck_cotizaciones_resolucion CHECK ((estado IN ('ACEPTADA', 'RECHAZADA'))
                                                    = (fecha_resolucion IS NOT NULL)),
    ADD CONSTRAINT ck_cotizaciones_rechazo CHECK (estado <> 'RECHAZADA' OR motivo_rechazo IS NOT NULL);

CREATE INDEX ix_cotizaciones_estado ON cotizaciones (estado, fecha_validez);

-- Una cotizacion solo se modifica en borrador: protege las lineas aunque se
-- escriba directamente en la base de datos.
CREATE OR REPLACE FUNCTION fn_cotizacion_lineas_editables() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_estado VARCHAR(20);
BEGIN
    SELECT estado INTO v_estado FROM cotizaciones
    WHERE id_cotizacion = COALESCE(NEW.id_cotizacion, OLD.id_cotizacion);
    IF v_estado IS DISTINCT FROM 'BORRADOR' THEN
        RAISE EXCEPTION 'La cotizacion % no esta en borrador: sus lineas no se pueden modificar',
            COALESCE(NEW.id_cotizacion, OLD.id_cotizacion)
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN COALESCE(NEW, OLD);
END $$;

CREATE TRIGGER trg_cotizacion_lineas_editables
BEFORE INSERT OR UPDATE OR DELETE ON cotizacion_servicios
FOR EACH ROW EXECUTE FUNCTION fn_cotizacion_lineas_editables();

-- ---------------------------------------------------------------------
-- Servicios, lugares y proyectos
-- ---------------------------------------------------------------------
ALTER TABLE servicios
    ADD COLUMN unidad_medida VARCHAR(30) NOT NULL DEFAULT 'servicio';

ALTER TABLE lugares
    ADD COLUMN ciudad VARCHAR(80);

ALTER TABLE proyectos
    ADD COLUMN descripcion TEXT;

CREATE INDEX ix_eventos_periodo ON eventos (fecha_inicio, fecha_fin);

-- ---------------------------------------------------------------------
-- Catalogos iniciales (editables desde la base de datos)
-- ---------------------------------------------------------------------
INSERT INTO tipos_cliente (nombre) VALUES
    ('Persona natural'),
    ('Empresa privada'),
    ('Institución pública');

INSERT INTO tipos_documento (nombre) VALUES
    ('Carnet de identidad'),
    ('NIT'),
    ('Pasaporte'),
    ('Otro');

INSERT INTO tipos_contacto (nombre) VALUES
    ('Celular'),
    ('Teléfono fijo'),
    ('Correo electrónico');
