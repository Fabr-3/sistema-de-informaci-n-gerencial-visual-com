-- =====================================================================
-- V7 - Modulo de logistica: personal tecnico, jornadas de montaje y
-- desmontaje y asignacion de personal a los eventos.
--   * Un evento puede tener varias jornadas de montaje y de desmontaje;
--     reemplazan a las columnas fecha_montaje y fecha_desmontaje del plan.
--   * Un tecnico puede tener asignaciones que se cruzan entre eventos: el
--     sistema solo lo alerta, por lo que se elimina la restriccion EXCLUDE.
--   * El personal tecnico se puede vincular a una cuenta de usuario para
--     que consulte sus propias asignaciones.
--   * El gerente tambien planifica la logistica.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Planes logisticos y jornadas
-- ---------------------------------------------------------------------
ALTER TABLE planes_logisticos DROP CONSTRAINT planes_logisticos_check;
ALTER TABLE planes_logisticos
    DROP COLUMN fecha_montaje,
    DROP COLUMN fecha_desmontaje;

CREATE TABLE jornadas_logisticas (
    id_jornada          SERIAL PRIMARY KEY,
    id_plan             INT NOT NULL REFERENCES planes_logisticos(id_plan) ON DELETE CASCADE,
    tipo                VARCHAR(20) NOT NULL CHECK (tipo IN ('MONTAJE', 'DESMONTAJE')),
    fecha_inicio        TIMESTAMP NOT NULL,
    fecha_fin           TIMESTAMP NOT NULL,
    descripcion         VARCHAR(500),
    creado_por          INT REFERENCES usuarios(id_usuario),
    modificado_por      INT REFERENCES usuarios(id_usuario),
    fecha_creacion      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion  TIMESTAMP,
    CHECK (fecha_fin > fecha_inicio)
);
CREATE INDEX ix_jornadas_plan ON jornadas_logisticas (id_plan, fecha_inicio);

-- ---------------------------------------------------------------------
-- Personal tecnico
-- ---------------------------------------------------------------------
ALTER TABLE personal_tecnico
    ADD COLUMN telefono   VARCHAR(30),
    ADD COLUMN activo     BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN id_usuario INT UNIQUE REFERENCES usuarios(id_usuario);

-- ---------------------------------------------------------------------
-- Asignaciones: los cruces se alertan, no se bloquean
-- ---------------------------------------------------------------------
ALTER TABLE asignaciones_personal DROP CONSTRAINT asignaciones_personal_id_personal_tsrange_excl;
ALTER TABLE asignaciones_personal
    ADD COLUMN creado_por         INT REFERENCES usuarios(id_usuario),
    ADD COLUMN modificado_por     INT REFERENCES usuarios(id_usuario),
    ADD COLUMN fecha_creacion     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN fecha_modificacion TIMESTAMP;
CREATE INDEX ix_asignaciones_personal_periodo
    ON asignaciones_personal (id_personal, fecha_inicio_actividad, fecha_fin_actividad);

-- ---------------------------------------------------------------------
-- Catalogos iniciales (editables desde la base de datos)
-- ---------------------------------------------------------------------
INSERT INTO especialidades_tecnicas (nombre) VALUES
    ('Sonido'),
    ('Iluminación'),
    ('Pantallas LED y video'),
    ('Estructuras y rigging'),
    ('Electricidad y generación'),
    ('Transporte')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO roles_evento (nombre) VALUES
    ('Jefe de montaje'),
    ('Técnico de sonido'),
    ('Técnico de iluminación'),
    ('Técnico de pantallas LED'),
    ('Operador de video'),
    ('Rigger'),
    ('Electricista'),
    ('Chofer'),
    ('Ayudante de montaje')
ON CONFLICT (nombre) DO NOTHING;

-- ---------------------------------------------------------------------
-- Permisos: el gerente tambien planifica la logistica
-- ---------------------------------------------------------------------
INSERT INTO roles_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r JOIN permisos p ON p.nombre = 'LOGISTICA_GESTIONAR'
WHERE r.codigo = 'GERENTE'
ON CONFLICT DO NOTHING;
