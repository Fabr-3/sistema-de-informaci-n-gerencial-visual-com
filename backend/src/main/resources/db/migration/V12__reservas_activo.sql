-- =====================================================================
-- V12 - Reserva de activos por evento (logistica).
--   * Cada reserva guarda su periodo (salida y retorno) y su estado; una
--     reserva liberada conserva la fecha y el motivo.
--   * Un activo liberado se puede volver a reservar en el mismo plan.
--   * Un activo no puede tener dos reservas activas superpuestas.
-- =====================================================================

ALTER TABLE reservas_activo
    ADD COLUMN id_reserva          SERIAL,
    ADD COLUMN fecha_salida        TIMESTAMP,
    ADD COLUMN fecha_retorno       TIMESTAMP,
    ADD COLUMN estado              VARCHAR(15) NOT NULL DEFAULT 'ACTIVA',
    ADD COLUMN fecha_liberacion    TIMESTAMP,
    ADD COLUMN motivo_liberacion   VARCHAR(255),
    ADD COLUMN modificado_por      INT REFERENCES usuarios(id_usuario),
    ADD COLUMN fecha_modificacion  TIMESTAMP;

-- Periodo de las reservas previas: del primer montaje (o el inicio del
-- evento) al ultimo desmontaje (o el fin del evento)
UPDATE reservas_activo r
SET fecha_salida  = COALESCE((SELECT MIN(j.fecha_inicio) FROM jornadas_logisticas j
                              WHERE j.id_plan = r.id_plan AND j.tipo = 'MONTAJE'), e.fecha_inicio),
    fecha_retorno = COALESCE((SELECT MAX(j.fecha_fin) FROM jornadas_logisticas j
                              WHERE j.id_plan = r.id_plan AND j.tipo = 'DESMONTAJE'), e.fecha_fin)
FROM planes_logisticos p
JOIN eventos e ON e.id_evento = p.id_evento
WHERE p.id_plan = r.id_plan;

ALTER TABLE reservas_activo
    ALTER COLUMN fecha_salida SET NOT NULL,
    ALTER COLUMN fecha_retorno SET NOT NULL,
    DROP CONSTRAINT reservas_activo_pkey,
    ADD PRIMARY KEY (id_reserva),
    ADD CONSTRAINT ck_reservas_periodo CHECK (fecha_retorno > fecha_salida),
    ADD CONSTRAINT ck_reservas_estado CHECK (estado IN ('ACTIVA', 'LIBERADA')),
    ADD CONSTRAINT ck_reservas_liberacion CHECK (estado = 'ACTIVA' OR fecha_liberacion IS NOT NULL),
    ADD CONSTRAINT ex_reservas_superpuestas EXCLUDE USING gist (
        id_activo WITH =,
        tsrange(fecha_salida, fecha_retorno, '[)') WITH &&
    ) WHERE (estado = 'ACTIVA');

CREATE UNIQUE INDEX ux_reservas_activa_plan ON reservas_activo (id_plan, id_activo) WHERE estado = 'ACTIVA';
CREATE INDEX ix_reservas_plan ON reservas_activo (id_plan);
