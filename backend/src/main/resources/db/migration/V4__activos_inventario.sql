-- =====================================================================
-- V4 - Modulo de gestion de activos e inventario.
--   * Bodegas donde se resguardan los activos.
--   * Tipo de control por modelo: UNIDAD (cada equipo con codigo propio)
--     o LOTE (cantidades, p. ej. cables y accesorios).
--   * Intervalo de mantenimiento preventivo por horas de uso (por modelo).
--   * Historial de uso de cada activo y vista de horas acumuladas.
--   * Existencias y movimientos de los activos controlados por lote.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Bodegas
-- ---------------------------------------------------------------------
CREATE TABLE bodegas (
    id_bodega           SERIAL PRIMARY KEY,
    nombre              VARCHAR(100) NOT NULL UNIQUE,
    direccion           TEXT,
    activa              BOOLEAN NOT NULL DEFAULT TRUE,
    creado_por          INT REFERENCES usuarios(id_usuario),
    modificado_por      INT REFERENCES usuarios(id_usuario),
    fecha_creacion      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion  TIMESTAMP
);

-- ---------------------------------------------------------------------
-- Modelos: tipo de control e intervalo de mantenimiento
-- ---------------------------------------------------------------------
-- horas_mantenimiento: horas de uso entre mantenimientos preventivos. Solo
-- aplica a activos controlados por unidad (los lotes no registran horas).
ALTER TABLE modelos_activo
    ADD COLUMN tipo_control        VARCHAR(10) NOT NULL DEFAULT 'UNIDAD',
    ADD COLUMN horas_mantenimiento INT,
    ADD CONSTRAINT ck_modelos_tipo_control CHECK (tipo_control IN ('UNIDAD', 'LOTE')),
    ADD CONSTRAINT ck_modelos_horas_mantenimiento CHECK (horas_mantenimiento IS NULL OR horas_mantenimiento > 0),
    ADD CONSTRAINT ck_modelos_horas_solo_unidad CHECK (tipo_control = 'UNIDAD' OR horas_mantenimiento IS NULL);

-- ---------------------------------------------------------------------
-- Activos controlados por unidad
-- ---------------------------------------------------------------------
ALTER TABLE activos
    ADD COLUMN id_bodega          INT NOT NULL REFERENCES bodegas(id_bodega),
    ADD COLUMN fecha_adquisicion  DATE,
    ADD COLUMN costo_adquisicion  NUMERIC(12,2),
    ADD COLUMN observaciones      TEXT,
    ADD COLUMN motivo_baja        TEXT,
    ADD CONSTRAINT ck_activos_costo CHECK (costo_adquisicion IS NULL OR costo_adquisicion >= 0),
    ADD CONSTRAINT ck_activos_motivo_baja CHECK (estado_operativo <> 'BAJA' OR motivo_baja IS NOT NULL);
CREATE INDEX ix_activos_bodega ON activos (id_bodega);

ALTER TABLE modulos_led
    ADD COLUMN modificado_por      INT REFERENCES usuarios(id_usuario),
    ADD COLUMN fecha_modificacion  TIMESTAMP;

-- ---------------------------------------------------------------------
-- Mantenimiento
-- ---------------------------------------------------------------------
-- reinicia_horas: un mantenimiento de este tipo, al finalizar, reinicia el
-- contador de horas para el siguiente mantenimiento preventivo.
ALTER TABLE tipos_mantenimiento
    ADD COLUMN reinicia_horas BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE mantenimientos
    ADD COLUMN costo NUMERIC(12,2),
    ADD CONSTRAINT ck_mantenimientos_costo CHECK (costo IS NULL OR costo >= 0),
    ADD CONSTRAINT ck_mantenimientos_condicion
        CHECK (condicion_final IS NULL
               OR condicion_final IN ('OPERATIVO', 'OPERATIVO_CON_OBSERVACIONES', 'NO_REPARABLE'));

-- Un activo solo puede tener un mantenimiento en proceso
CREATE UNIQUE INDEX ux_mantenimientos_en_proceso ON mantenimientos (id_activo)
    WHERE estado = 'EN_PROCESO' AND fecha_eliminacion IS NULL;

-- ---------------------------------------------------------------------
-- Historial de uso
-- ---------------------------------------------------------------------
-- id_evento es opcional: el modulo de logistica lo completara cuando el uso
-- provenga de un evento planificado.
CREATE TABLE registros_uso (
    id_registro_uso     SERIAL PRIMARY KEY,
    id_activo           INT NOT NULL REFERENCES activos(id_activo) ON DELETE RESTRICT,
    id_evento           INT REFERENCES eventos(id_evento) ON DELETE RESTRICT,
    fecha_inicio        TIMESTAMP NOT NULL,
    fecha_fin           TIMESTAMP NOT NULL,
    descripcion         VARCHAR(255),
    creado_por          INT REFERENCES usuarios(id_usuario),
    fecha_creacion      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    eliminado_por       INT REFERENCES usuarios(id_usuario),
    fecha_eliminacion   TIMESTAMP,
    CHECK (fecha_fin > fecha_inicio),
    -- Un activo no puede estar en uso en dos lugares al mismo tiempo
    EXCLUDE USING gist (
        id_activo WITH =,
        tsrange(fecha_inicio, fecha_fin, '[)') WITH &&
    ) WHERE (fecha_eliminacion IS NULL)
);
CREATE INDEX ix_registros_uso_activo ON registros_uso (id_activo, fecha_inicio DESC);

-- Horas de uso por activo: totales y desde el ultimo mantenimiento que
-- reinicia el contador. Es un dato derivado, por eso es una vista.
CREATE VIEW v_uso_activos AS
WITH ultimo_mantenimiento AS (
    SELECT m.id_activo, MAX(m.fecha_fin) AS fecha_ultimo_mantenimiento
    FROM mantenimientos m
    JOIN tipos_mantenimiento t ON t.id_tipo_mantenimiento = m.id_tipo_mantenimiento
    WHERE m.estado = 'FINALIZADO' AND m.fecha_eliminacion IS NULL AND t.reinicia_horas
    GROUP BY m.id_activo
)
SELECT a.id_activo,
       COALESCE(SUM(EXTRACT(EPOCH FROM (r.fecha_fin - r.fecha_inicio)) / 3600), 0)::NUMERIC(12,2)
           AS horas_totales,
       COALESCE(SUM(EXTRACT(EPOCH FROM (r.fecha_fin - r.fecha_inicio)) / 3600)
                    FILTER (WHERE u.fecha_ultimo_mantenimiento IS NULL
                                  OR r.fecha_inicio >= u.fecha_ultimo_mantenimiento), 0)::NUMERIC(12,2)
           AS horas_desde_mantenimiento,
       u.fecha_ultimo_mantenimiento
FROM activos a
LEFT JOIN registros_uso r ON r.id_activo = a.id_activo AND r.fecha_eliminacion IS NULL
LEFT JOIN ultimo_mantenimiento u ON u.id_activo = a.id_activo
GROUP BY a.id_activo, u.fecha_ultimo_mantenimiento;

-- ---------------------------------------------------------------------
-- Activos controlados por lote
-- ---------------------------------------------------------------------
-- cantidad es el saldo vigente; se mantiene junto a los movimientos para
-- que el CHECK impida existencias negativas sin recalcular el historial.
CREATE TABLE existencias_lote (
    id_existencia  SERIAL PRIMARY KEY,
    id_modelo      INT NOT NULL REFERENCES modelos_activo(id_modelo),
    id_bodega      INT NOT NULL REFERENCES bodegas(id_bodega),
    cantidad       INT NOT NULL DEFAULT 0 CHECK (cantidad >= 0),
    UNIQUE (id_modelo, id_bodega)
);

-- cantidad lleva signo: positiva incrementa la existencia, negativa la reduce.
CREATE TABLE movimientos_lote (
    id_movimiento   SERIAL PRIMARY KEY,
    id_existencia   INT NOT NULL REFERENCES existencias_lote(id_existencia) ON DELETE RESTRICT,
    tipo            VARCHAR(20) NOT NULL
                    CHECK (tipo IN ('INGRESO', 'SALIDA', 'RETORNO', 'BAJA', 'AJUSTE',
                                    'TRASLADO_SALIDA', 'TRASLADO_ENTRADA')),
    cantidad        INT NOT NULL CHECK (cantidad <> 0),
    motivo          VARCHAR(255),
    fecha           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creado_por      INT REFERENCES usuarios(id_usuario)
);
CREATE INDEX ix_movimientos_lote_existencia ON movimientos_lote (id_existencia, fecha DESC);

-- ---------------------------------------------------------------------
-- Datos iniciales (editables desde la aplicacion)
-- ---------------------------------------------------------------------
INSERT INTO tipos_mantenimiento (nombre, reinicia_horas) VALUES
    ('Preventivo', TRUE),
    ('Correctivo', FALSE);

INSERT INTO categorias_activo (nombre) VALUES
    ('Pantalla LED'),
    ('Procesamiento de video'),
    ('Iluminación'),
    ('Sonido'),
    ('Estructuras y rigging'),
    ('Energía y cableado');

INSERT INTO bodegas (nombre) VALUES
    ('Bodega 1'),
    ('Bodega 2');
