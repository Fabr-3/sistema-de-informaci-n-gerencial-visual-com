-- =====================================================================
-- Datos de prueba: subcontratacion y consumibles.
-- Requiere la migracion V14 y los scripts 01 a 06 cargados.
-- Uso:
--   psql -h localhost -U postgres -d proyecto-visual-com -f database/datos_prueba/07_subcontratacion.sql
-- * Las dos subcontrataciones registradas en 04_finanzas se vinculan a su
--   egreso; se agregan otras con su egreso y una anulada (egreso anulado).
-- * Cinco consumibles con su compra inicial; la cinta gaffer queda bajo el
--   minimo. Consumos en eventos pasados, a su costo copiado, y un ajuste.
-- =====================================================================
\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM consumibles) OR EXISTS (SELECT 1 FROM subcontrataciones) THEN
        RAISE EXCEPTION 'Los datos de prueba de subcontratacion ya fueron cargados';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM movimientos_financieros WHERE concepto = 'Armado de truss y escenario') THEN
        RAISE EXCEPTION 'Cargue primero 04_finanzas.sql';
    END IF;
END $$;

CREATE TEMP TABLE ref ON COMMIT DROP AS
SELECT (SELECT id_usuario FROM usuarios WHERE nombre_usuario = 'admin')    AS admin,
       (SELECT id_usuario FROM usuarios WHERE nombre_usuario = 'jmamani')  AS tecnico,
       (SELECT id_usuario FROM usuarios WHERE nombre_usuario = 'rcondori') AS tecnico2,
       (SELECT id_tipo_movimiento FROM tipos_movimiento WHERE codigo_sistema = 'SUBCONTRATACION') AS tipo;

-- ---------------------------------------------------------------------
-- Subcontrataciones
-- ---------------------------------------------------------------------
-- Egresos registrados en 04_finanzas antes de que existiera el modulo
INSERT INTO subcontrataciones (id_proveedor, id_servicio_prov, servicio, id_proyecto, id_evento, descripcion, costo,
                               fecha, id_movimiento, creado_por, fecha_creacion)
SELECT m.id_proveedor, s.id_servicio_prov, s.descripcion_servicio, m.id_proyecto, m.id_evento, x.descripcion,
       m.monto, m.fecha_movimiento, m.id_movimiento, m.creado_por, m.fecha_creacion
FROM (VALUES
    ('Armado de truss y escenario', 'Truss y escenarios',
     'Armado de truss de 12 x 8 m y escenario principal con dos operarios'),
    ('Estructura del stand y armado', 'Armado de stands',
     'Estructura metálica del stand de 9 x 6 m, armado y desarmado')
) AS x (concepto, servicio, descripcion)
JOIN movimientos_financieros m ON m.concepto = x.concepto AND m.id_tipo_movimiento = (SELECT tipo FROM ref)
JOIN servicios_proveedor s ON s.id_proveedor = m.id_proveedor AND s.descripcion_servicio = x.servicio;

-- Nuevas subcontrataciones con su egreso imputado al proyecto y evento
CREATE TEMP TABLE nuevas ON COMMIT DROP AS
SELECT pr.id_proveedor, s.id_servicio_prov, s.descripcion_servicio, e.id_proyecto, e.id_evento, x.descripcion,
       x.costo, x.fecha, x.anulada
FROM (VALUES
    ('Generadores Andinos S.R.L.', 'Generador eléctrico', 'Festival universitario de bandas',
     'Generador de 100 kVA con operador para el escenario al aire libre', 2200.00, DATE '2026-10-24', FALSE),
    ('Alquiladora Sonido Total', 'Microfonía inalámbrica', 'Lanzamiento de línea automotriz 2027',
     'Ocho micrófonos inalámbricos de solapa para presentadores', 1500.00, DATE '2026-10-08', FALSE),
    ('Transportes Huayna S.R.L.', 'Camión con grúa', 'Cena de gala de fin de año',
     'Camión con grúa para izar la pantalla central (reservado con anticipo)', 1900.00, DATE '2026-11-12', FALSE),
    ('Generadores Andinos S.R.L.', 'Generador eléctrico', 'Congreso Nacional de Banca',
     'Generador de respaldo para el salón principal', 1800.00, DATE '2026-06-10', TRUE)
) AS x (proveedor, servicio, evento, descripcion, costo, fecha, anulada)
JOIN proveedores pr ON pr.razon_social = x.proveedor
JOIN servicios_proveedor s ON s.id_proveedor = pr.id_proveedor AND s.descripcion_servicio = x.servicio
JOIN eventos e ON e.nombre_evento = x.evento;

WITH egresos AS (
    INSERT INTO movimientos_financieros (id_tipo_movimiento, concepto, monto, fecha_movimiento, id_proyecto,
                                         id_evento, id_proveedor, estado, motivo_anulacion, creado_por,
                                         fecha_creacion)
    SELECT ref.tipo, 'Subcontratación · ' || pr.razon_social || ' · ' || n.descripcion_servicio, n.costo, n.fecha,
           n.id_proyecto, n.id_evento, n.id_proveedor,
           CASE WHEN n.anulada THEN 'ANULADO' ELSE 'REGISTRADO' END,
           CASE WHEN n.anulada THEN 'El hotel proporcionó su propio generador de respaldo' END,
           ref.admin, n.fecha + TIME '10:00'
    FROM nuevas n
    JOIN proveedores pr ON pr.id_proveedor = n.id_proveedor
    CROSS JOIN ref
    RETURNING id_movimiento, id_evento, id_proveedor
)
INSERT INTO subcontrataciones (id_proveedor, id_servicio_prov, servicio, id_proyecto, id_evento, descripcion, costo,
                               fecha, estado, motivo_anulacion, id_movimiento, creado_por, fecha_creacion)
SELECT n.id_proveedor, n.id_servicio_prov, n.descripcion_servicio, n.id_proyecto, n.id_evento, n.descripcion,
       n.costo, n.fecha, CASE WHEN n.anulada THEN 'ANULADA' ELSE 'REGISTRADA' END,
       CASE WHEN n.anulada THEN 'El hotel proporcionó su propio generador de respaldo' END,
       g.id_movimiento, ref.admin, n.fecha + TIME '10:00'
FROM nuevas n
JOIN egresos g ON g.id_evento = n.id_evento AND g.id_proveedor = n.id_proveedor
CROSS JOIN ref;

-- ---------------------------------------------------------------------
-- Consumibles, compra inicial y consumos en eventos pasados
-- ---------------------------------------------------------------------
INSERT INTO consumibles (nombre, unidad_medida, stock_minimo, costo_unitario, creado_por, fecha_creacion)
SELECT x.nombre, x.unidad, x.minimo, x.costo, ref.admin, TIMESTAMP '2026-06-01 09:00'
FROM (VALUES
    ('Cinta gaffer negra 2"',            'rollo',   6, 45.00),
    ('Pilas AA alcalinas',               'unidad', 20,  6.50),
    ('Precintos plásticos 30 cm',        'paquete', 3, 28.00),
    ('Cinta de embalaje transparente',   'rollo',   5, 12.00),
    ('Alcohol isopropílico',             'litro',   2, 55.00)
) AS x (nombre, unidad, minimo, costo)
CROSS JOIN ref;

INSERT INTO movimientos_consumible (id_consumible, tipo, cantidad, fecha, motivo, creado_por)
SELECT c.id_consumible, 'INGRESO', x.cantidad, TIMESTAMP '2026-06-01 09:30', 'Compra inicial', ref.admin
FROM (VALUES
    ('Cinta gaffer negra 2"', 24), ('Pilas AA alcalinas', 120), ('Precintos plásticos 30 cm', 10),
    ('Cinta de embalaje transparente', 20), ('Alcohol isopropílico', 8)
) AS x (nombre, cantidad)
JOIN consumibles c ON c.nombre = x.nombre
CROSS JOIN ref;

CREATE TEMP TABLE consumos_prueba ON COMMIT DROP AS
SELECT c.id_consumible, e.id_proyecto, e.id_evento, x.cantidad, c.costo_unitario, e.fecha_inicio::DATE AS fecha,
       x.tecnico, e.nombre_evento
FROM (VALUES
    ('Cinta gaffer negra 2"',          'Congreso Nacional de Banca',           4, 1),
    ('Pilas AA alcalinas',             'Congreso Nacional de Banca',          32, 1),
    ('Cinta gaffer negra 2"',          'Concierto aniversario 25 años',        8, 2),
    ('Pilas AA alcalinas',             'Concierto aniversario 25 años',       40, 2),
    ('Precintos plásticos 30 cm',      'Concierto aniversario 25 años',        3, 2),
    ('Cinta de embalaje transparente', 'Montaje del stand',                    6, 1),
    ('Precintos plásticos 30 cm',      'Montaje del stand',                    2, 1),
    ('Cinta gaffer negra 2"',          'Lanzamiento de línea automotriz 2027', 3, 2),
    ('Alcohol isopropílico',           'Lanzamiento de línea automotriz 2027', 2, 2),
    ('Cinta gaffer negra 2"',          'Festival universitario de bandas',     5, 1),
    ('Pilas AA alcalinas',             'Festival universitario de bandas',    24, 1)
) AS x (consumible, evento, cantidad, tecnico)
JOIN consumibles c ON c.nombre = x.consumible
JOIN eventos e ON e.nombre_evento = x.evento;

INSERT INTO consumos_proyecto (id_consumible, id_proyecto, id_evento, cantidad, costo_unitario, fecha,
                               registrado_por, fecha_creacion)
SELECT k.id_consumible, k.id_proyecto, k.id_evento, k.cantidad, k.costo_unitario, k.fecha,
       CASE k.tecnico WHEN 1 THEN ref.tecnico ELSE ref.tecnico2 END, k.fecha + TIME '23:30'
FROM consumos_prueba k CROSS JOIN ref;

INSERT INTO movimientos_consumible (id_consumible, tipo, cantidad, fecha, motivo, creado_por)
SELECT k.id_consumible, 'CONSUMO', -k.cantidad, k.fecha + TIME '23:30', 'Consumo · ' || k.nombre_evento,
       CASE k.tecnico WHEN 1 THEN ref.tecnico ELSE ref.tecnico2 END
FROM consumos_prueba k CROSS JOIN ref;

-- Conteo fisico: dos rollos de cinta de embalaje danados por humedad
INSERT INTO movimientos_consumible (id_consumible, tipo, cantidad, fecha, motivo, creado_por)
SELECT c.id_consumible, 'AJUSTE', -2, TIMESTAMP '2026-10-30 17:00', 'Conteo físico: rollos dañados por humedad',
       ref.admin
FROM consumibles c CROSS JOIN ref
WHERE c.nombre = 'Cinta de embalaje transparente';

-- El stock vigente es la suma de los movimientos
UPDATE consumibles c
SET stock_actual = s.total, modificado_por = ref.admin, fecha_modificacion = TIMESTAMP '2026-10-30 17:00'
FROM (SELECT id_consumible, SUM(cantidad) AS total FROM movimientos_consumible GROUP BY id_consumible) s, ref
WHERE s.id_consumible = c.id_consumible;

COMMIT;
