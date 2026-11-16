-- =====================================================================
-- Datos de prueba: reserva de equipos para los eventos proximos.
-- Requiere la migracion V12 y los scripts 01 a 05 cargados.
-- Uso:
--   psql -h localhost -U postgres -d proyecto-visual-com -f database/datos_prueba/06_reservas.sql
-- El periodo de cada reserva va del primer montaje (o el inicio del
-- evento) al ultimo desmontaje (o el fin del evento), igual que en la
-- aplicacion. Las pantallas Roe del Carnaval estan reservadas el sabado y
-- el domingo (reservas futuras que no se superponen). ABPL-0021 se reservo
-- antes de entrar a mantenimiento: queda en riesgo para el palco.
-- =====================================================================
\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM reservas_activo) THEN
        RAISE EXCEPTION 'Los datos de prueba de reservas ya fueron cargados';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM planes_logisticos p JOIN eventos e ON e.id_evento = p.id_evento
                   WHERE e.nombre_evento = 'Cena de gala de fin de año') THEN
        RAISE EXCEPTION 'Cargue primero 03_logistica.sql';
    END IF;
END $$;

CREATE TEMP TABLE reservas_prueba (evento TEXT, prefijo TEXT, desde INT, hasta INT, creada TIMESTAMP)
    ON COMMIT DROP;

INSERT INTO reservas_prueba VALUES
    ('Montaje de pantallas e iluminación', 'ABPL',  9, 12, TIMESTAMP '2026-11-03 10:15'),
    ('Montaje de pantallas e iluminación', 'CHMK3', 1,  4, TIMESTAMP '2026-11-03 10:15'),
    ('Cena de gala de fin de año',         'ABPL',  1,  8, TIMESTAMP '2026-11-04 16:40'),
    ('Cena de gala de fin de año',         'NVMC',  1,  1, TIMESTAMP '2026-11-04 16:40'),
    ('Cena de gala de fin de año',         'LAK2',  1,  4, TIMESTAMP '2026-11-04 16:40'),
    ('Cena de gala de fin de año',         'RBMP',  1,  6, TIMESTAMP '2026-11-04 16:40'),
    ('Montaje en el palco',                'SHULX', 1,  3, TIMESTAMP '2026-10-20 09:30'),
    ('Montaje en el palco',                'YCL5',  1,  1, TIMESTAMP '2026-10-20 09:30'),
    ('Montaje en el palco',                'ABPL', 21, 21, TIMESTAMP '2026-10-20 09:30'),
    ('Entrada del Carnaval · sábado',      'ROEBM', 1,  6, TIMESTAMP '2026-10-20 09:45'),
    ('Entrada del Carnaval · domingo',     'ROEBM', 1,  6, TIMESTAMP '2026-10-20 09:45'),
    ('Entrada del Carnaval · domingo',     'NVMC',  2,  2, TIMESTAMP '2026-10-20 09:45');

INSERT INTO reservas_activo (id_plan, id_activo, fecha_salida, fecha_retorno, creado_por, fecha_creacion)
SELECT p.id_plan, a.id_activo,
       COALESCE((SELECT MIN(j.fecha_inicio) FROM jornadas_logisticas j
                 WHERE j.id_plan = p.id_plan AND j.tipo = 'MONTAJE'), e.fecha_inicio),
       COALESCE((SELECT MAX(j.fecha_fin) FROM jornadas_logisticas j
                 WHERE j.id_plan = p.id_plan AND j.tipo = 'DESMONTAJE'), e.fecha_fin),
       (SELECT id_usuario FROM usuarios WHERE nombre_usuario = 'mvargas'), r.creada
FROM reservas_prueba r
JOIN eventos e ON e.nombre_evento = r.evento
JOIN planes_logisticos p ON p.id_evento = e.id_evento
CROSS JOIN LATERAL generate_series(r.desde, r.hasta) AS n(numero)
JOIN activos a ON a.codigo_inventario = r.prefijo || '-' || LPAD(n.numero::TEXT, 4, '0');

COMMIT;
