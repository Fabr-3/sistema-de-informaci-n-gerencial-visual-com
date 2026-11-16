-- =====================================================================
-- V13 - Historial de los modulos LED.
--   * Cada alta, cambio de estado, reemplazo o reasignacion de un modulo
--     queda registrado con la pantalla de origen y destino, los estados y
--     el modulo relacionado (el que reemplaza o es reemplazado).
--   * Se registra el alta de los modulos existentes en su pantalla actual,
--     con el estado que tienen hoy (los cambios previos no se conocen).
-- =====================================================================

CREATE TABLE historial_modulos_led (
    id_historial           SERIAL PRIMARY KEY,
    id_modulo              INT NOT NULL REFERENCES modulos_led(id_modulo) ON DELETE RESTRICT,
    tipo                   VARCHAR(20) NOT NULL
                           CHECK (tipo IN ('ALTA', 'CAMBIO_ESTADO', 'REEMPLAZO', 'REASIGNACION')),
    id_activo_origen       INT REFERENCES activos(id_activo),
    id_activo_destino      INT REFERENCES activos(id_activo),
    estado_anterior        VARCHAR(20) CHECK (estado_anterior IN ('ACTIVO', 'DEFECTUOSO', 'RETIRADO')),
    estado_nuevo           VARCHAR(20) CHECK (estado_nuevo IN ('ACTIVO', 'DEFECTUOSO', 'RETIRADO')),
    id_modulo_relacionado  INT REFERENCES modulos_led(id_modulo),
    motivo                 TEXT,
    fecha                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    registrado_por         INT REFERENCES usuarios(id_usuario),
    CONSTRAINT ck_historial_reasignacion
        CHECK (tipo <> 'REASIGNACION' OR (id_activo_origen IS NOT NULL AND id_activo_destino IS NOT NULL
                                          AND id_activo_origen <> id_activo_destino)),
    CONSTRAINT ck_historial_reemplazo CHECK (tipo <> 'REEMPLAZO' OR id_modulo_relacionado IS NOT NULL)
);
CREATE INDEX ix_historial_modulos_modulo ON historial_modulos_led (id_modulo, fecha);

INSERT INTO historial_modulos_led (id_modulo, tipo, id_activo_destino, estado_nuevo, fecha, registrado_por)
SELECT m.id_modulo, 'ALTA', m.id_activo_padre, m.estado, m.fecha_creacion, m.creado_por
FROM modulos_led m
WHERE m.fecha_eliminacion IS NULL;
