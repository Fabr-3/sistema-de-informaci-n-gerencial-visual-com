-- =====================================================================
-- Datos de prueba: modulo de informacion gerencial.
-- Requiere las migraciones hasta V11 y los scripts 01 a 04 cargados.
-- Uso:
--   psql -h localhost -U postgres -d proyecto-visual-com -f database/datos_prueba/05_gerencial.sql
-- Completa el historial de mantenimiento para que el tablero muestre todos
-- los tipos de alerta de activos:
--   * ROEBM-0003 suma tres correctivos en el ultimo año (fallas recurrentes).
--   * ABPL-0021 lleva mas de una semana en mantenimiento (prolongado).
-- Los costos de los correctivos se registran como egresos, igual que lo hace
-- la aplicacion al finalizar un mantenimiento. Los KPIs, la utilizacion y
-- los reportes se calculan con los datos de los scripts anteriores.
-- =====================================================================
\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM tipos_movimiento WHERE codigo_sistema = 'MANTENIMIENTO') THEN
        RAISE EXCEPTION 'Aplique primero las migraciones hasta V11 (inicie el backend)';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM activos WHERE codigo_inventario IN ('ROEBM-0003', 'ABPL-0021')
                   HAVING COUNT(*) = 2) THEN
        RAISE EXCEPTION 'Cargue primero 01_accesos_activos.sql';
    END IF;
    IF EXISTS (SELECT 1 FROM mantenimientos WHERE descripcion = 'Falla de alimentación en la fuente de poder') THEN
        RAISE EXCEPTION 'Los datos de prueba gerenciales ya fueron cargados';
    END IF;
    IF EXISTS (SELECT 1 FROM activos WHERE codigo_inventario = 'ABPL-0021' AND estado_operativo <> 'DISPONIBLE') THEN
        RAISE EXCEPTION 'ABPL-0021 debe estar disponible para iniciar su mantenimiento';
    END IF;
END $$;

CREATE TEMP TABLE ref ON COMMIT DROP AS
SELECT (SELECT id_usuario FROM usuarios WHERE nombre_usuario = 'rcondori') AS tecnico,
       (SELECT id_tipo_mantenimiento FROM tipos_mantenimiento WHERE nombre = 'Correctivo') AS correctivo,
       (SELECT id_tipo_movimiento FROM tipos_movimiento WHERE codigo_sistema = 'MANTENIMIENTO') AS egreso;

-- Correctivos finalizados de ROEBM-0003 (con el de agosto de 01 suman tres)
WITH nuevos AS (
    INSERT INTO mantenimientos (id_activo, id_tipo_mantenimiento, fecha_inicio, fecha_fin, descripcion,
                                observaciones, condicion_final, costo, estado, creado_por, fecha_creacion,
                                modificado_por, fecha_modificacion)
    SELECT a.id_activo, ref.correctivo, m.inicio, m.fin, m.descripcion, m.observaciones, 'OPERATIVO', m.costo,
           'FINALIZADO', ref.tecnico, m.inicio, ref.tecnico, m.fin
    FROM ref, activos a, (VALUES
        (TIMESTAMP '2026-09-29 10:00', TIMESTAMP '2026-09-30 16:00', 'Falla de alimentación en la fuente de poder',
         'Se reemplazó la fuente; revisar el cableado del gabinete', 620.00),
        (TIMESTAMP '2026-10-19 09:00', TIMESTAMP '2026-10-19 18:00', 'Módulo receptor sin señal tras el evento',
         'Tarjeta receptora reemplazada', 940.00)
    ) AS m(inicio, fin, descripcion, observaciones, costo)
    WHERE a.codigo_inventario = 'ROEBM-0003'
    RETURNING id_mantenimiento, fecha_fin, costo, modificado_por
)
INSERT INTO movimientos_financieros (id_tipo_movimiento, concepto, monto, fecha_movimiento, id_mantenimiento,
                                     creado_por, fecha_creacion)
SELECT ref.egreso, 'Mantenimiento correctivo · ROEBM-0003', n.costo, n.fecha_fin::DATE, n.id_mantenimiento,
       n.modificado_por, n.fecha_fin
FROM ref, nuevos n;

-- Mantenimiento en proceso desde hace mas de una semana
INSERT INTO mantenimientos (id_activo, id_tipo_mantenimiento, fecha_inicio, descripcion, estado, creado_por,
                            fecha_creacion)
SELECT a.id_activo, ref.correctivo, TIMESTAMP '2026-10-28 11:00',
       'Gabinete deformado; en espera de repuesto del fabricante', 'EN_PROCESO', ref.tecnico,
       TIMESTAMP '2026-10-28 11:00'
FROM ref, activos a
WHERE a.codigo_inventario = 'ABPL-0021';

UPDATE activos SET estado_operativo = 'EN_MANTENIMIENTO', modificado_por = ref.tecnico,
                   fecha_modificacion = TIMESTAMP '2026-10-28 11:00'
FROM ref
WHERE codigo_inventario = 'ABPL-0021';

COMMIT;
