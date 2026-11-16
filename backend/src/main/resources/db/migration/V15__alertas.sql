-- =====================================================================
-- V15 - Alertas persistentes.
--   * Las alertas se siguen calculando sobre los datos; al sincronizarse
--     se registran como PENDIENTE, el gerente las marca como ATENDIDA y pasan
--     a RESUELTA cuando la situacion ya no se presenta.
--   * clave identifica la situacion concreta dentro de su tipo (activo,
--     mantenimiento, reserva o consumible): solo una alerta abierta por
--     tipo y clave.
-- =====================================================================

CREATE TABLE alertas (
    id_alerta            BIGSERIAL PRIMARY KEY,
    tipo                 VARCHAR(30) NOT NULL
                         CHECK (tipo IN ('RESERVA_EN_RIESGO', 'MANTENIMIENTO_VENCIDO', 'MANTENIMIENTO_PROXIMO',
                                         'MANTENIMIENTO_PROLONGADO', 'MODULOS_DEFECTUOSOS', 'FALLAS_RECURRENTES',
                                         'SIN_USO', 'STOCK_BAJO')),
    severidad            VARCHAR(15) NOT NULL CHECK (severidad IN ('CRITICA', 'ADVERTENCIA', 'INFORMATIVA')),
    id_activo            INT REFERENCES activos(id_activo),
    id_evento            INT REFERENCES eventos(id_evento),
    clave                VARCHAR(100) NOT NULL,
    detalle              TEXT NOT NULL,
    fecha_generacion     TIMESTAMP NOT NULL,
    estado               VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE'
                         CHECK (estado IN ('PENDIENTE', 'ATENDIDA', 'RESUELTA')),
    atendida_por         INT REFERENCES usuarios(id_usuario),
    fecha_atencion       TIMESTAMP,
    comentario_atencion  TEXT,
    fecha_resolucion     TIMESTAMP,
    CONSTRAINT ck_alertas_atencion CHECK (estado <> 'ATENDIDA' OR fecha_atencion IS NOT NULL),
    CONSTRAINT ck_alertas_resolucion CHECK (estado <> 'RESUELTA' OR fecha_resolucion IS NOT NULL)
);

CREATE UNIQUE INDEX ux_alertas_abiertas ON alertas (tipo, clave) WHERE estado <> 'RESUELTA';
CREATE INDEX ix_alertas_estado ON alertas (estado, fecha_generacion DESC);
