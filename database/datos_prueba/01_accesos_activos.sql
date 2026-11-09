-- =====================================================================
-- Datos de prueba: modulos de accesos y de activos e inventario.
-- Uso (base de desarrollo, con las migraciones ya aplicadas):
--   psql -h localhost -U postgres -d proyecto-visual-com -f database/datos_prueba/01_accesos_activos.sql
-- Contrasena de todos los usuarios de prueba: VisualCom2026
-- Los datos se insertan directamente (sin pasar por la API), por lo que no
-- generan registros de auditoria. No modifica datos existentes.
-- =====================================================================
\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM usuarios WHERE nombre_usuario = 'mvargas') THEN
        RAISE EXCEPTION 'Los datos de prueba de accesos y activos ya fueron cargados';
    END IF;
END $$;

-- Referencias reutilizadas por el script
CREATE TEMP TABLE ref ON COMMIT DROP AS
SELECT (SELECT id_usuario FROM usuarios WHERE nombre_usuario = 'admin')               AS admin,
       (SELECT id_bodega FROM bodegas ORDER BY id_bodega LIMIT 1)                     AS bodega_principal,
       (SELECT id_bodega FROM bodegas ORDER BY id_bodega OFFSET 1 LIMIT 1)            AS bodega_secundaria,
       (SELECT id_tipo_mantenimiento FROM tipos_mantenimiento WHERE nombre = 'Preventivo') AS preventivo,
       (SELECT id_tipo_mantenimiento FROM tipos_mantenimiento WHERE nombre = 'Correctivo') AS correctivo;

-- ---------------------------------------------------------------------
-- Usuarios (hash BCrypt de "VisualCom2026")
-- ---------------------------------------------------------------------
INSERT INTO usuarios (nombre_usuario, nombres, apellido_paterno, apellido_materno, email, password_hash, id_rol,
                      fecha_expiracion, debe_cambiar_contrasena, creado_por, fecha_creacion)
SELECT u.nombre_usuario, u.nombres, u.paterno, u.materno, u.email,
       '$2a$12$O01p8TfIJ7mgrU2bvw/MoeU3fiJlzlIrggOxUPFf1.TXdR8NAD3Fu',
       (SELECT id_rol FROM roles WHERE codigo = u.rol), u.expiracion, FALSE, ref.admin, u.alta
FROM ref, (VALUES
    ('mvargas',    'Marcelo',  'Vargas',    'Rocha',   'mvargas@visual.com.bo',    'GERENTE',                NULL::TIMESTAMP,                TIMESTAMP '2026-01-12 09:00'),
    ('lquispe',    'Lucía',    'Quispe',    'Mamani',  'lquispe@visual.com.bo',    'GERENTE',                NULL,                           TIMESTAMP '2026-01-12 09:10'),
    ('jmamani',    'Javier',   'Mamani',    'Choque',  'jmamani@visual.com.bo',    'TECNICO',                NULL,                           TIMESTAMP '2026-01-15 10:00'),
    ('rcondori',   'Rodrigo',  'Condori',   'Flores',  'rcondori@visual.com.bo',   'TECNICO',                NULL,                           TIMESTAMP '2026-01-15 10:05'),
    ('dgutierrez', 'Daniela',  'Gutiérrez', 'Arce',    'dgutierrez@visual.com.bo', 'TECNICO',                NULL,                           TIMESTAMP '2026-02-02 08:30'),
    ('ftorrez',    'Fernando', 'Torrez',    'Aliaga',  'ftorrez@visual.com.bo',    'SUBCONTRATADO_TEMPORAL', TIMESTAMP '2027-03-31 23:59:59', TIMESTAMP '2026-10-01 08:00'),
    ('cmendoza',   'Carla',    'Mendoza',   NULL,      'cmendoza@visual.com.bo',   'SUBCONTRATADO_TEMPORAL', TIMESTAMP '2026-08-31 23:59:59', TIMESTAMP '2026-05-02 08:00')
) AS u(nombre_usuario, nombres, paterno, materno, email, rol, expiracion, alta);

-- ---------------------------------------------------------------------
-- Catalogos
-- ---------------------------------------------------------------------
INSERT INTO fabricantes (nombre) VALUES
    ('Absen'), ('ROE Visual'), ('NovaStar'), ('Shure'), ('Yamaha'), ('L-Acoustics'),
    ('Robe'), ('Chauvet Professional'), ('Global Truss'), ('Klotz'), ('Neutrik')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO modelos_activo (id_fabricante, id_categoria, nombre, tipo_control, horas_mantenimiento, prefijo_codigo)
SELECT f.id_fabricante, c.id_categoria, m.nombre, m.tipo, m.horas, m.prefijo
FROM (VALUES
    ('Absen',                'Pantalla LED',           'PL3.9 Pro V2',         'UNIDAD', 1500, 'ABPL'),
    ('ROE Visual',           'Pantalla LED',           'Black Marble BM4',     'UNIDAD', 1200, 'ROEBM'),
    ('NovaStar',             'Procesamiento de video', 'MCTRL4K',              'UNIDAD', 4000, 'NVMC'),
    ('Shure',                'Sonido',                 'ULXD24/SM58',          'UNIDAD', 2000, 'SHULX'),
    ('Yamaha',               'Sonido',                 'CL5',                  'UNIDAD', 3000, 'YCL5'),
    ('L-Acoustics',          'Sonido',                 'K2',                   'UNIDAD', 1500, 'LAK2'),
    ('Robe',                 'Iluminación',            'MegaPointe',           'UNIDAD',  750, 'RBMP'),
    ('Chauvet Professional', 'Iluminación',            'Maverick MK3 Wash',    'UNIDAD', 1000, 'CHMK3'),
    ('Global Truss',         'Estructuras y rigging',  'F34 3 m',              'LOTE',   NULL, NULL),
    ('Klotz',                'Energía y cableado',     'Cable DMX 5 pines 10 m', 'LOTE', NULL, NULL),
    ('Neutrik',              'Energía y cableado',     'Cable powerCON TRUE1 5 m', 'LOTE', NULL, NULL),
    ('Neutrik',              'Energía y cableado',     'Cable etherCON Cat6 30 m', 'LOTE', NULL, NULL)
) AS m(fabricante, categoria, nombre, tipo, horas, prefijo)
JOIN fabricantes f ON f.nombre = m.fabricante
JOIN categorias_activo c ON c.nombre = m.categoria;

INSERT INTO especificaciones_modelo (id_modelo, caracteristica, valor)
SELECT mo.id_modelo, e.caracteristica, e.valor
FROM (VALUES
    ('ABPL',  'Pitch', '3.9 mm'), ('ABPL', 'Gabinete', '500 × 500 mm'), ('ABPL', 'Brillo', '4500 nits'),
    ('ABPL',  'Consumo máximo', '180 W'), ('ABPL', 'Uso', 'Exterior IP65'),
    ('ROEBM', 'Pitch', '4.76 mm'), ('ROEBM', 'Gabinete', '500 × 500 mm'), ('ROEBM', 'Carga máxima', '1500 kg/m²'),
    ('NVMC',  'Resolución máxima', '4096 × 2160'), ('NVMC', 'Salidas', '16 puertos Gigabit'),
    ('SHULX', 'Tipo', 'Micrófono inalámbrico de mano'), ('SHULX', 'Banda', 'G50 (470–534 MHz)'),
    ('YCL5',  'Canales', '72 mono + 8 estéreo'), ('YCL5', 'Red', 'Dante'),
    ('LAK2',  'Tipo', 'Line array de 3 vías'), ('LAK2', 'SPL máximo', '147 dB'),
    ('RBMP',  'Fuente', 'Lámpara 470 W'), ('RBMP', 'Zoom', '1.8° – 21°'),
    ('CHMK3', 'Fuente', '27 LED RGBW de 40 W'), ('CHMK3', 'Zoom', '5° – 55°')
) AS e(prefijo, caracteristica, valor)
JOIN modelos_activo mo ON mo.prefijo_codigo = e.prefijo;

-- ---------------------------------------------------------------------
-- Activos por unidad (codigos PREFIJO-NNNN)
-- Un tercio de las unidades se resguarda en la bodega secundaria.
-- ---------------------------------------------------------------------
INSERT INTO activos (codigo_inventario, id_modelo, numero_serie, estado_operativo, id_bodega, fecha_adquisicion,
                     costo_adquisicion, creado_por, fecha_creacion)
SELECT d.prefijo || '-' || lpad(n::TEXT, 4, '0'), mo.id_modelo, d.serie || lpad((d.serie_base + n)::TEXT, 5, '0'),
       'DISPONIBLE',
       CASE WHEN n > d.cantidad * 2 / 3 THEN ref.bodega_secundaria ELSE ref.bodega_principal END,
       d.adquisicion, d.costo, ref.admin, TIMESTAMP '2026-01-20 09:00'
FROM ref, (VALUES
    ('ABPL',  24, 'PL39V2-23A', 10400, DATE '2023-08-14',   7650.00),
    ('ROEBM', 12, 'BM4-2402-',   3300, DATE '2024-03-05',   9800.00),
    ('NVMC',   2, 'MC4K-B',       880, DATE '2023-08-14',  31500.00),
    ('SHULX',  8, 'ULXD-3T',     5120, DATE '2024-06-21',  14200.00),
    ('YCL5',   1, 'CL5-BX',       710, DATE '2022-11-30', 238000.00),
    ('LAK2',   8, 'K2-21-',      1840, DATE '2025-02-17', 162000.00),
    ('RBMP',  12, 'MPT-',        6630, DATE '2023-04-10',  89000.00),
    ('CHMK3',  8, 'MK3W-',       2275, DATE '2024-09-02',  41500.00)
) AS d(prefijo, cantidad, serie, serie_base, adquisicion, costo)
JOIN modelos_activo mo ON mo.prefijo_codigo = d.prefijo
CROSS JOIN LATERAL generate_series(1, d.cantidad) AS n;

-- Modulos LED: cuatro por gabinete
INSERT INTO modulos_led (id_activo_padre, codigo_modulo, estado, creado_por, fecha_creacion)
SELECT a.id_activo, a.codigo_inventario || '-M' || m, 'ACTIVO', ref.admin, a.fecha_creacion
FROM ref, activos a
JOIN modelos_activo mo ON mo.id_modelo = a.id_modelo AND mo.prefijo_codigo IN ('ABPL', 'ROEBM')
CROSS JOIN generate_series(1, 4) AS m;

UPDATE modulos_led SET estado = 'DEFECTUOSO', fecha_modificacion = TIMESTAMP '2026-10-20 11:00'
WHERE codigo_modulo IN ('ABPL-0003-M2', 'ABPL-0011-M4', 'ROEBM-0007-M1');
UPDATE modulos_led SET estado = 'RETIRADO', fecha_modificacion = TIMESTAMP '2026-09-03 16:00'
WHERE codigo_modulo IN ('ABPL-0024-M1', 'ABPL-0024-M3');

-- ---------------------------------------------------------------------
-- Historial de uso en eventos (jornada continua por equipo y evento)
-- Las unidades 1 a 6 de RBMP quedan reservadas para la instalacion fija.
-- ---------------------------------------------------------------------
CREATE TEMP TABLE eventos_uso (nombre TEXT, inicio TIMESTAMP, horas INT, prefijo TEXT, desde INT, hasta INT) ON COMMIT DROP;
INSERT INTO eventos_uso VALUES
    ('Entrada folklórica del Gran Poder 2026 · palco Av. Montes', '2026-05-30 08:00', 16, 'ABPL', 1, 16),
    ('Entrada folklórica del Gran Poder 2026 · palco Av. Montes', '2026-05-30 08:00', 16, 'NVMC', 1, 1),
    ('Entrada folklórica del Gran Poder 2026 · palco Av. Montes', '2026-05-30 08:00', 16, 'LAK2', 1, 8),
    ('Entrada folklórica del Gran Poder 2026 · palco Av. Montes', '2026-05-30 08:00', 16, 'YCL5', 1, 1),
    ('Entrada folklórica del Gran Poder 2026 · palco Av. Montes', '2026-05-30 08:00', 16, 'SHULX', 1, 6),
    ('Congreso Nacional de Banca · Hotel Casa Grande',            '2026-06-18 07:00', 11, 'ABPL', 1, 12),
    ('Congreso Nacional de Banca · Hotel Casa Grande',            '2026-06-18 07:00', 11, 'NVMC', 2, 2),
    ('Congreso Nacional de Banca · Hotel Casa Grande',            '2026-06-18 07:00', 11, 'SHULX', 1, 8),
    ('Congreso Nacional de Banca · Hotel Casa Grande',            '2026-06-18 07:00', 11, 'CHMK3', 1, 4),
    ('Concierto aniversario · Teatro al Aire Libre Jaime Laredo', '2026-07-16 15:00', 9, 'ABPL', 1, 24),
    ('Concierto aniversario · Teatro al Aire Libre Jaime Laredo', '2026-07-16 15:00', 9, 'NVMC', 1, 2),
    ('Concierto aniversario · Teatro al Aire Libre Jaime Laredo', '2026-07-16 15:00', 9, 'LAK2', 1, 8),
    ('Concierto aniversario · Teatro al Aire Libre Jaime Laredo', '2026-07-16 15:00', 9, 'YCL5', 1, 1),
    ('Concierto aniversario · Teatro al Aire Libre Jaime Laredo', '2026-07-16 15:00', 9, 'RBMP', 7, 12),
    ('Concierto aniversario · Teatro al Aire Libre Jaime Laredo', '2026-07-16 15:00', 9, 'CHMK3', 1, 8),
    ('Feria Internacional FEXPOCRUZ 2026 · stand corporativo',    '2026-09-18 09:00', 12, 'ROEBM', 1, 12),
    ('Feria Internacional FEXPOCRUZ 2026 · stand corporativo',    '2026-09-18 09:00', 12, 'ABPL', 13, 20),
    ('Feria Internacional FEXPOCRUZ 2026 · stand corporativo',    '2026-09-18 09:00', 12, 'NVMC', 2, 2),
    ('Lanzamiento de línea automotriz · Megacenter Irpavi',       '2026-10-09 17:00', 7, 'ROEBM', 1, 8),
    ('Lanzamiento de línea automotriz · Megacenter Irpavi',       '2026-10-09 17:00', 7, 'CHMK3', 1, 6),
    ('Lanzamiento de línea automotriz · Megacenter Irpavi',       '2026-10-09 17:00', 7, 'SHULX', 1, 4),
    ('Festival universitario de bandas · Coliseo Cerrado',        '2026-10-24 16:00', 8, 'LAK2', 1, 8),
    ('Festival universitario de bandas · Coliseo Cerrado',        '2026-10-24 16:00', 8, 'YCL5', 1, 1),
    ('Festival universitario de bandas · Coliseo Cerrado',        '2026-10-24 16:00', 8, 'RBMP', 7, 12),
    ('Festival universitario de bandas · Coliseo Cerrado',        '2026-10-24 16:00', 8, 'ABPL', 1, 16),
    ('Festival universitario de bandas · Coliseo Cerrado',        '2026-10-24 16:00', 8, 'NVMC', 1, 1);

INSERT INTO registros_uso (id_activo, fecha_inicio, fecha_fin, descripcion, creado_por, fecha_creacion)
SELECT a.id_activo, e.inicio, e.inicio + e.horas * INTERVAL '1 hour', e.nombre,
       (SELECT id_usuario FROM usuarios WHERE nombre_usuario = 'jmamani'),
       e.inicio + e.horas * INTERVAL '1 hour' + INTERVAL '1 day'
FROM eventos_uso e
JOIN modelos_activo mo ON mo.prefijo_codigo = e.prefijo
JOIN activos a ON a.id_modelo = mo.id_modelo
WHERE substring(a.codigo_inventario FROM '-([0-9]+)$')::INT BETWEEN e.desde AND e.hasta;

-- Instalacion fija nocturna (8 h diarias): lleva a RBMP-0001..0004 cerca del
-- intervalo de 750 h (alerta proxima) y a RBMP-0005..0006 por encima (vencido).
INSERT INTO registros_uso (id_activo, fecha_inicio, fecha_fin, descripcion, creado_por, fecha_creacion)
SELECT a.id_activo, dia + TIME '21:00', dia + TIME '21:00' + INTERVAL '8 hours',
       'Instalación fija · discoteca zona Sur (contrato de temporada)',
       (SELECT id_usuario FROM usuarios WHERE nombre_usuario = 'rcondori'), dia + INTERVAL '1 day'
FROM activos a
JOIN modelos_activo mo ON mo.id_modelo = a.id_modelo AND mo.prefijo_codigo = 'RBMP'
CROSS JOIN LATERAL generate_series(
        DATE '2026-11-07' - CASE WHEN substring(a.codigo_inventario FROM '-([0-9]+)$')::INT <= 4 THEN 87 ELSE 99 END,
        DATE '2026-11-07', INTERVAL '1 day') AS dia
WHERE substring(a.codigo_inventario FROM '-([0-9]+)$')::INT <= 6;

-- Registro anulado por error de carga (no cuenta para las horas)
UPDATE registros_uso SET fecha_eliminacion = TIMESTAMP '2026-06-19 10:00',
                         eliminado_por = (SELECT id_usuario FROM usuarios WHERE nombre_usuario = 'jmamani')
WHERE id_registro_uso = (SELECT min(r.id_registro_uso) FROM registros_uso r
                         JOIN activos a ON a.id_activo = r.id_activo
                         WHERE a.codigo_inventario = 'SHULX-0007');

-- ---------------------------------------------------------------------
-- Mantenimientos
-- ---------------------------------------------------------------------
INSERT INTO mantenimientos (id_activo, id_tipo_mantenimiento, fecha_inicio, fecha_fin, descripcion, observaciones,
                            condicion_final, costo, estado, creado_por, fecha_creacion)
SELECT a.id_activo, CASE m.tipo WHEN 'P' THEN ref.preventivo ELSE ref.correctivo END, m.inicio, m.fin, m.descripcion,
       m.observaciones, m.condicion, m.costo, CASE WHEN m.fin IS NULL THEN 'EN_PROCESO' ELSE 'FINALIZADO' END,
       (SELECT id_usuario FROM usuarios WHERE nombre_usuario = m.tecnico), m.inicio
FROM ref, (VALUES
    ('ABPL-0001', 'P', TIMESTAMP '2026-04-08 09:00', TIMESTAMP '2026-04-08 17:00', 'Limpieza de módulos, ajuste de conectores y calibración de color', NULL, 'OPERATIVO', 450.00, 'dgutierrez'),
    ('ABPL-0002', 'P', TIMESTAMP '2026-04-08 09:00', TIMESTAMP '2026-04-08 17:00', 'Limpieza de módulos, ajuste de conectores y calibración de color', NULL, 'OPERATIVO', 450.00, 'dgutierrez'),
    ('ABPL-0003', 'C', TIMESTAMP '2026-10-21 10:00', TIMESTAMP '2026-10-22 12:00', 'Píxeles apagados en módulo 2', 'Módulo M2 marcado defectuoso; pendiente de reposición por el proveedor', 'OPERATIVO_CON_OBSERVACIONES', 0.00, 'jmamani'),
    ('ABPL-0024', 'C', TIMESTAMP '2026-09-01 09:00', TIMESTAMP '2026-09-03 16:00', 'Gabinete golpeado durante el desmontaje', 'Estructura de aluminio deformada; no es posible su reparación', 'NO_REPARABLE', 300.00, 'jmamani'),
    ('ROEBM-0003','C', TIMESTAMP '2026-08-20 14:00', TIMESTAMP '2026-08-21 11:00', 'Reemplazo de vidrio superior por fisura', NULL, 'OPERATIVO', 1850.00, 'rcondori'),
    ('YCL5-0001', 'P', TIMESTAMP '2026-03-02 09:00', TIMESTAMP '2026-03-02 13:00', 'Actualización de firmware y limpieza de faders', NULL, 'OPERATIVO', 0.00, 'dgutierrez'),
    ('RBMP-0007', 'P', TIMESTAMP '2026-05-12 09:00', TIMESTAMP '2026-05-13 12:00', 'Cambio de lámpara y limpieza óptica', NULL, 'OPERATIVO', 2600.00, 'rcondori'),
    ('CHMK3-0002','P', TIMESTAMP '2026-11-06 09:30', NULL,                         'Limpieza de ventiladores y revisión de motores de zoom', NULL, NULL, NULL, 'rcondori'),
    ('SHULX-0005','C', TIMESTAMP '2026-11-07 15:00', NULL,                         'Ruido intermitente en la cápsula', NULL, NULL, NULL, 'jmamani')
) AS m(codigo, tipo, inicio, fin, descripcion, observaciones, condicion, costo, tecnico)
JOIN activos a ON a.codigo_inventario = m.codigo;

UPDATE activos SET estado_operativo = 'EN_MANTENIMIENTO', fecha_modificacion = TIMESTAMP '2026-11-06 09:30'
WHERE codigo_inventario IN ('CHMK3-0002', 'SHULX-0005');
UPDATE activos SET estado_operativo = 'BAJA', fecha_modificacion = TIMESTAMP '2026-09-03 16:00',
                   motivo_baja = 'Estructura de aluminio deformada; no es posible su reparación'
WHERE codigo_inventario = 'ABPL-0024';
UPDATE activos SET estado_operativo = 'BAJA', fecha_modificacion = TIMESTAMP '2026-06-20 10:00',
                   motivo_baja = 'Extraviado al cierre del Congreso Nacional de Banca; informe interno 07/2026'
WHERE codigo_inventario = 'SHULX-0008';
UPDATE activos SET observaciones = 'Instalado en rack de procesamiento principal' WHERE codigo_inventario = 'NVMC-0001';

-- ---------------------------------------------------------------------
-- Inventario por lotes: movimientos y saldos resultantes
-- ---------------------------------------------------------------------
INSERT INTO existencias_lote (id_modelo, id_bodega, cantidad)
SELECT mo.id_modelo, b.id_bodega, 0
FROM ref, modelos_activo mo
JOIN fabricantes f ON f.id_fabricante = mo.id_fabricante
CROSS JOIN bodegas b
WHERE mo.tipo_control = 'LOTE' AND b.id_bodega IN (ref.bodega_principal, ref.bodega_secundaria)
  AND f.nombre IN ('Global Truss', 'Klotz', 'Neutrik');

INSERT INTO movimientos_lote (id_existencia, tipo, cantidad, motivo, fecha, creado_por)
SELECT e.id_existencia, m.tipo, m.cantidad, m.motivo, m.fecha, ref.admin
FROM (VALUES
    ('F34 3 m',                   1, 'INGRESO',           24, 'Compra de tramos de truss F34',                     TIMESTAMP '2025-03-10 10:00'),
    ('F34 3 m',                   1, 'TRASLADO_SALIDA',   -8, 'Traslado a bodega secundaria',                      TIMESTAMP '2025-03-12 09:00'),
    ('F34 3 m',                   2, 'TRASLADO_ENTRADA',   8, 'Traslado desde bodega principal',                   TIMESTAMP '2025-03-12 09:00'),
    ('F34 3 m',                   1, 'SALIDA',           -12, 'Entrada folklórica del Gran Poder 2026',            TIMESTAMP '2026-05-29 18:00'),
    ('F34 3 m',                   1, 'RETORNO',           12, 'Retorno Gran Poder 2026',                           TIMESTAMP '2026-06-01 11:00'),
    ('Cable DMX 5 pines 10 m',    1, 'INGRESO',           60, 'Compra anual de cableado',                          TIMESTAMP '2025-01-20 10:00'),
    ('Cable DMX 5 pines 10 m',    1, 'TRASLADO_SALIDA',  -15, 'Traslado a bodega secundaria',                      TIMESTAMP '2025-01-22 09:00'),
    ('Cable DMX 5 pines 10 m',    2, 'TRASLADO_ENTRADA',  15, 'Traslado desde bodega principal',                   TIMESTAMP '2025-01-22 09:00'),
    ('Cable DMX 5 pines 10 m',    1, 'SALIDA',           -24, 'Concierto aniversario · Teatro al Aire Libre',      TIMESTAMP '2026-07-15 16:00'),
    ('Cable DMX 5 pines 10 m',    1, 'RETORNO',           22, 'Retorno concierto aniversario',                     TIMESTAMP '2026-07-17 12:00'),
    ('Cable DMX 5 pines 10 m',    1, 'BAJA',              -2, 'Conectores XLR dañados en el desmontaje',          TIMESTAMP '2026-07-17 12:30'),
    ('Cable powerCON TRUE1 5 m',  1, 'INGRESO',           80, 'Compra anual de cableado',                          TIMESTAMP '2025-01-20 10:00'),
    ('Cable powerCON TRUE1 5 m',  1, 'TRASLADO_SALIDA',  -20, 'Traslado a bodega secundaria',                      TIMESTAMP '2025-01-22 09:00'),
    ('Cable powerCON TRUE1 5 m',  2, 'TRASLADO_ENTRADA',  20, 'Traslado desde bodega principal',                   TIMESTAMP '2025-01-22 09:00'),
    ('Cable powerCON TRUE1 5 m',  1, 'SALIDA',           -30, 'Feria Internacional FEXPOCRUZ 2026',                TIMESTAMP '2026-09-16 08:00'),
    ('Cable powerCON TRUE1 5 m',  1, 'RETORNO',           29, 'Retorno FEXPOCRUZ 2026',                            TIMESTAMP '2026-09-23 15:00'),
    ('Cable powerCON TRUE1 5 m',  1, 'BAJA',              -1, 'Extraviado en FEXPOCRUZ 2026',                      TIMESTAMP '2026-09-23 15:10'),
    ('Cable etherCON Cat6 30 m',  1, 'INGRESO',           20, 'Compra para tendido de señal de pantallas',         TIMESTAMP '2025-02-05 10:00'),
    ('Cable etherCON Cat6 30 m',  1, 'AJUSTE',            -1, 'Diferencia en conteo físico de inventario',         TIMESTAMP '2026-10-31 17:00')
) AS m(modelo, bodega, tipo, cantidad, motivo, fecha)
CROSS JOIN ref
JOIN modelos_activo mo ON mo.nombre = m.modelo
JOIN existencias_lote e ON e.id_modelo = mo.id_modelo
     AND e.id_bodega = CASE m.bodega WHEN 1 THEN ref.bodega_principal ELSE ref.bodega_secundaria END;

UPDATE existencias_lote e
SET cantidad = (SELECT COALESCE(SUM(m.cantidad), 0) FROM movimientos_lote m WHERE m.id_existencia = e.id_existencia)
WHERE e.id_existencia IN (SELECT id_existencia FROM movimientos_lote);

-- Las combinaciones modelo/bodega sin movimientos no forman parte del inventario
DELETE FROM existencias_lote e
WHERE NOT EXISTS (SELECT 1 FROM movimientos_lote m WHERE m.id_existencia = e.id_existencia);

COMMIT;
