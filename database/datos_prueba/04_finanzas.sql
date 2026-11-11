-- =====================================================================
-- Datos de prueba: modulo financiero (proveedores, ingresos y egresos).
-- Requiere las migraciones V8 a V10 y los scripts 01, 02 y 03 cargados.
-- Uso:
--   psql -h localhost -U postgres -d proyecto-visual-com -f database/datos_prueba/04_finanzas.sql
-- Incluye cobros y costos de los proyectos de 02_comercial.sql (por evento
-- cuando corresponde), gastos generales mensuales, alquileres de equipos sin
-- proyecto, una venta de cables con su salida de inventario y un cobro
-- anulado. La feria FEXPOCRUZ queda con margen bajo y el festival con saldo
-- por cobrar, para probar las alertas de los indicadores. Los costos de los
-- mantenimientos finalizados de 01_accesos_activos.sql se registran como
-- egresos, igual que lo hace la aplicacion al finalizar un mantenimiento.
-- =====================================================================
\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM movimientos_financieros) OR EXISTS (SELECT 1 FROM proveedores) THEN
        RAISE EXCEPTION 'Los datos de prueba financieros ya fueron cargados';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM proyectos WHERE nombre_proyecto = 'Cena de gala de fin de año') THEN
        RAISE EXCEPTION 'Cargue primero 02_comercial.sql';
    END IF;
END $$;

CREATE TEMP TABLE ref ON COMMIT DROP AS
SELECT (SELECT id_usuario FROM usuarios WHERE nombre_usuario = 'admin')   AS admin,
       (SELECT id_usuario FROM usuarios WHERE nombre_usuario = 'mvargas') AS gerente,
       (SELECT id_tipo_documento FROM tipos_documento WHERE nombre = 'NIT') AS nit;

-- ---------------------------------------------------------------------
-- Proveedores
-- ---------------------------------------------------------------------
CREATE TEMP TABLE proveedores_prueba ON COMMIT DROP AS
SELECT * FROM (VALUES
    ('Transportes Huayna S.R.L.',          '1029384025', '2 2841570', 'logistica@transporteshuayna.com.bo'),
    ('Generadores Andinos S.R.L.',         '1023456021', '72011450',  'alquileres@generadoresandinos.com.bo'),
    ('Estructuras Metálicas El Alto',      '4012457018', '71590233',  NULL),
    ('Alquiladora Sonido Total',           '3016789023', '2 2413388', 'reservas@sonidototal.bo'),
    ('Importadora LED Bolivia S.R.L.',     '1045678029', '3 3456120', 'ventas@ledbolivia.com.bo'),
    ('Ferretería Técnica Max Paredes',     NULL,         '76512044',  NULL)
) AS p(razon_social, nit, telefono, email);

INSERT INTO proveedores (razon_social, telefono, email, creado_por, fecha_creacion)
SELECT p.razon_social, p.telefono, p.email, ref.admin, TIMESTAMP '2026-01-08 10:00'
FROM ref, proveedores_prueba p;

INSERT INTO documentos_proveedor (id_proveedor, id_tipo_documento, numero, creado_por, fecha_creacion)
SELECT pr.id_proveedor, ref.nit, p.nit, ref.admin, TIMESTAMP '2026-01-08 10:00'
FROM ref, proveedores_prueba p
JOIN proveedores pr ON pr.razon_social = p.razon_social
WHERE p.nit IS NOT NULL;

INSERT INTO servicios_proveedor (id_proveedor, descripcion_servicio)
SELECT pr.id_proveedor, s.servicio
FROM (VALUES
    ('Transportes Huayna S.R.L.',      'Transporte de carga'),
    ('Transportes Huayna S.R.L.',      'Camión con grúa'),
    ('Generadores Andinos S.R.L.',     'Generador eléctrico'),
    ('Generadores Andinos S.R.L.',     'Torres de iluminación'),
    ('Estructuras Metálicas El Alto',  'Truss y escenarios'),
    ('Estructuras Metálicas El Alto',  'Armado de stands'),
    ('Alquiladora Sonido Total',       'Sistemas line array'),
    ('Alquiladora Sonido Total',       'Microfonía inalámbrica'),
    ('Importadora LED Bolivia S.R.L.', 'Módulos LED'),
    ('Importadora LED Bolivia S.R.L.', 'Procesadores de video'),
    ('Ferretería Técnica Max Paredes', 'Cables y conectores')
) AS s(proveedor, servicio)
JOIN proveedores pr ON pr.razon_social = s.proveedor;

-- ---------------------------------------------------------------------
-- Movimientos (proyecto, evento y proveedor por nombre; NULL = general)
-- ---------------------------------------------------------------------
CREATE TEMP TABLE movimientos_prueba ON COMMIT DROP AS
SELECT * FROM (VALUES
    -- Congreso Nacional de Banca 2026 (contratado Bs 12.130)
    ('Cobro de servicios', 'Anticipo del 50 %', 6065.00, DATE '2026-05-20',
     'Congreso Nacional de Banca 2026 · pantallas y sonido', NULL, NULL, '000412', FALSE),
    ('Transporte y viáticos', 'Flete de pantalla y sonido al hotel', 650.00, DATE '2026-06-17',
     'Congreso Nacional de Banca 2026 · pantallas y sonido', 'Montaje de pantalla y sonido', NULL, NULL, FALSE),
    ('Alquiler de equipos a terceros', 'Microfonía inalámbrica adicional', 900.00, DATE '2026-06-18',
     'Congreso Nacional de Banca 2026 · pantallas y sonido', 'Congreso Nacional de Banca', 'Alquiladora Sonido Total', 'A-2231', FALSE),
    ('Cobro de servicios', 'Saldo del contrato', 6065.00, DATE '2026-06-25',
     'Congreso Nacional de Banca 2026 · pantallas y sonido', NULL, NULL, '000431', FALSE),
    -- Concierto aniversario 25 anos (contratado Bs 32.900)
    ('Cobro de servicios', 'Anticipo del 50 %', 16450.00, DATE '2026-07-02',
     'Concierto aniversario 25 años · producción técnica integral', NULL, NULL, '000447', FALSE),
    ('Subcontratación de servicios', 'Armado de truss y escenario', 3800.00, DATE '2026-07-15',
     'Concierto aniversario 25 años · producción técnica integral', 'Montaje de escenario', 'Estructuras Metálicas El Alto', 'F-1189', FALSE),
    ('Alquiler de equipos a terceros', 'Line array adicional para torres de delay', 6500.00, DATE '2026-07-16',
     'Concierto aniversario 25 años · producción técnica integral', 'Concierto aniversario 25 años', 'Alquiladora Sonido Total', 'A-2287', FALSE),
    ('Alquiler de equipos a terceros', 'Generador de 100 kVA', 2200.00, DATE '2026-07-16',
     'Concierto aniversario 25 años · producción técnica integral', 'Concierto aniversario 25 años', 'Generadores Andinos S.R.L.', '7781', FALSE),
    ('Transporte y viáticos', 'Fletes de ida y vuelta (2 camiones)', 1200.00, DATE '2026-07-17',
     'Concierto aniversario 25 años · producción técnica integral', NULL, NULL, NULL, FALSE),
    ('Transporte y viáticos', 'Alimentación del personal técnico', 650.00, DATE '2026-07-17',
     'Concierto aniversario 25 años · producción técnica integral', 'Concierto aniversario 25 años', NULL, NULL, FALSE),
    ('Cobro de servicios', 'Saldo del contrato', 16450.00, DATE '2026-07-24',
     'Concierto aniversario 25 años · producción técnica integral', NULL, NULL, '000458', FALSE),
    -- Stand corporativo FEXPOCRUZ 2026 (contratado Bs 55.600, margen bajo)
    ('Cobro de servicios', 'Anticipo del 50 %', 27800.00, DATE '2026-09-05',
     'Stand corporativo FEXPOCRUZ 2026', NULL, NULL, '000476', FALSE),
    ('Transporte y viáticos', 'Transporte de equipos La Paz - Santa Cruz (ida y vuelta)', 9800.00, DATE '2026-09-14',
     'Stand corporativo FEXPOCRUZ 2026', 'Montaje del stand', 'Transportes Huayna S.R.L.', 'T-5530', FALSE),
    ('Subcontratación de servicios', 'Estructura del stand y armado', 18500.00, DATE '2026-09-16',
     'Stand corporativo FEXPOCRUZ 2026', 'Montaje del stand', 'Estructuras Metálicas El Alto', 'F-1240', FALSE),
    ('Alquiler de equipos a terceros', 'Generador de respaldo durante la feria', 4800.00, DATE '2026-09-27',
     'Stand corporativo FEXPOCRUZ 2026', 'Feria Internacional FEXPOCRUZ 2026', 'Generadores Andinos S.R.L.', '7912', FALSE),
    ('Transporte y viáticos', 'Hospedaje y viáticos del personal (12 días)', 14600.00, DATE '2026-09-28',
     'Stand corporativo FEXPOCRUZ 2026', 'Feria Internacional FEXPOCRUZ 2026', NULL, NULL, FALSE),
    ('Cobro de servicios', 'Saldo del contrato', 27800.00, DATE '2026-10-05',
     'Stand corporativo FEXPOCRUZ 2026', NULL, NULL, '000489', FALSE),
    -- Lanzamiento de linea automotriz 2027 (contratado Bs 14.250)
    ('Transporte y viáticos', 'Flete del piso LED', 700.00, DATE '2026-10-09',
     'Lanzamiento de línea automotriz 2027', 'Lanzamiento de línea automotriz 2027', NULL, NULL, FALSE),
    ('Transporte y viáticos', 'Refrigerios del personal de montaje', 300.00, DATE '2026-10-09',
     'Lanzamiento de línea automotriz 2027', 'Lanzamiento de línea automotriz 2027', NULL, NULL, FALSE),
    ('Cobro de servicios', 'Pago total del servicio', 14250.00, DATE '2026-10-14',
     'Lanzamiento de línea automotriz 2027', NULL, NULL, '000495', FALSE),
    -- Festival universitario de bandas (contratado Bs 23.730, saldo pendiente)
    ('Cobro de servicios', 'Anticipo del 50 %', 11865.00, DATE '2026-10-15',
     'Festival universitario de bandas · sonido y pantallas', NULL, NULL, '000497', FALSE),
    ('Cobro de servicios', 'Saldo del contrato', 11865.00, DATE '2026-10-16',
     'Festival universitario de bandas · sonido y pantallas', NULL, NULL, '000498', TRUE),
    ('Alquiler de equipos a terceros', 'Sistema line array para el coliseo', 4200.00, DATE '2026-10-24',
     'Festival universitario de bandas · sonido y pantallas', 'Festival universitario de bandas', 'Alquiladora Sonido Total', 'A-2410', FALSE),
    ('Alquiler de equipos a terceros', 'Generador de 60 kVA', 1900.00, DATE '2026-10-24',
     'Festival universitario de bandas · sonido y pantallas', 'Festival universitario de bandas', 'Generadores Andinos S.R.L.', '8034', FALSE),
    ('Transporte y viáticos', 'Fletes al coliseo', 800.00, DATE '2026-10-25',
     'Festival universitario de bandas · sonido y pantallas', 'Festival universitario de bandas', NULL, NULL, FALSE),
    -- Proyectos en planificacion: anticipos
    ('Cobro de servicios', 'Anticipo del 50 %', 9350.00, DATE '2026-11-05',
     'Cena de gala de fin de año', NULL, NULL, '000503', FALSE),
    ('Cobro de servicios', 'Anticipo del 40 %', 15544.00, DATE '2026-11-06',
     'Activación de marca · Carnaval de Oruro 2027', NULL, NULL, '000504', FALSE),
    ('Transporte y viáticos', 'Reserva de hospedaje en Oruro para el equipo', 3200.00, DATE '2026-11-07',
     'Activación de marca · Carnaval de Oruro 2027', NULL, NULL, NULL, FALSE),
    -- Alquileres de equipos sin proyecto (alquiler en seco a otras productoras)
    ('Alquiler de equipos', 'Pantalla LED para Producciones Altura', 6800.00, DATE '2026-01-24', NULL, NULL, NULL, '000351', FALSE),
    ('Alquiler de equipos', 'Pantallas para corso de Carnaval paceño', 9500.00, DATE '2026-02-14', NULL, NULL, NULL, '000362', FALSE),
    ('Alquiler de equipos', 'Sonido para feria escolar', 3900.00, DATE '2026-03-21', NULL, NULL, NULL, '000370', FALSE),
    ('Otros ingresos', 'Capacitación en operación de pantallas LED', 1500.00, DATE '2026-03-10', NULL, NULL, NULL, '000366', FALSE),
    ('Alquiler de equipos', 'Iluminación para obra de teatro', 5200.00, DATE '2026-04-30', NULL, NULL, NULL, '000384', FALSE),
    ('Alquiler de equipos', 'Pantalla y sonido para congreso médico', 7400.00, DATE '2026-05-16', NULL, NULL, NULL, '000401', FALSE),
    ('Alquiler de equipos', 'Pantalla para transmisión deportiva', 4600.00, DATE '2026-08-08', NULL, NULL, NULL, '000465', FALSE),
    ('Alquiler de equipos', 'Sonido para kermés vecinal', 3800.00, DATE '2026-09-12', NULL, NULL, NULL, '000479', FALSE),
    ('Alquiler de equipos', 'Pantalla para lanzamiento de libro', 2900.00, DATE '2026-10-03', NULL, NULL, NULL, '000486', FALSE),
    -- Compras de equipos y materiales
    ('Compra de equipos y materiales', 'Procesador de video NovaStar VX1000', 14800.00, DATE '2026-02-18',
     NULL, NULL, 'Importadora LED Bolivia S.R.L.', 'LB-0098', FALSE),
    ('Compra de equipos y materiales', 'Cables y conectores para reposición', 3650.00, DATE '2026-04-22',
     NULL, NULL, 'Ferretería Técnica Max Paredes', NULL, FALSE),
    ('Compra de equipos y materiales', 'Módulos LED P3.9 de repuesto (24 unidades)', 12800.00, DATE '2026-10-28',
     NULL, NULL, 'Importadora LED Bolivia S.R.L.', 'LB-0157', FALSE)
) AS x(tipo, concepto, monto, fecha, proyecto, evento, proveedor, factura, anulado);

-- Gastos generales de cada mes: alquiler de oficina y bodegas y servicios basicos
INSERT INTO movimientos_prueba
SELECT 'Gastos generales', 'Alquiler de oficina y bodegas', 4500.00, (DATE '2026-01-05' + make_interval(months => n))::DATE,
       NULL, NULL, NULL, NULL, FALSE
FROM generate_series(0, 10) AS n;

INSERT INTO movimientos_prueba
SELECT 'Gastos generales', 'Servicios básicos (energía, agua e internet)', 780.00 + (n % 3) * 95,
       (DATE '2026-01-12' + make_interval(months => n))::DATE, NULL, NULL, NULL, NULL, FALSE
FROM generate_series(0, 9) AS n;

INSERT INTO movimientos_financieros (id_proyecto, id_evento, id_tipo_movimiento, concepto, monto, fecha_movimiento,
                                     id_proveedor, numero_factura, estado, motivo_anulacion, creado_por,
                                     fecha_creacion, modificado_por, fecha_modificacion)
SELECT p.id_proyecto, e.id_evento, t.id_tipo_movimiento, x.concepto, x.monto, x.fecha, pr.id_proveedor, x.factura,
       CASE WHEN x.anulado THEN 'ANULADO' ELSE 'REGISTRADO' END,
       CASE WHEN x.anulado THEN 'El cliente aún no pagó el saldo; se registró por error' END,
       CASE WHEN x.fecha >= DATE '2026-10-01' THEN ref.gerente ELSE ref.admin END,
       x.fecha + TIME '17:30',
       CASE WHEN x.anulado THEN ref.gerente END,
       CASE WHEN x.anulado THEN x.fecha + TIME '18:10' END
FROM ref, movimientos_prueba x
JOIN tipos_movimiento t ON t.nombre = x.tipo
LEFT JOIN proyectos p ON p.nombre_proyecto = x.proyecto
LEFT JOIN eventos e ON e.nombre_evento = x.evento AND e.id_proyecto = p.id_proyecto
LEFT JOIN proveedores pr ON pr.razon_social = x.proveedor;

-- ---------------------------------------------------------------------
-- Venta de 5 cables DMX usados de Bodega 2 (descuenta la existencia)
-- ---------------------------------------------------------------------
CREATE TEMP TABLE venta_prueba ON COMMIT DROP AS
SELECT ex.id_existencia, 5 AS cantidad
FROM existencias_lote ex
JOIN modelos_activo m ON m.id_modelo = ex.id_modelo
JOIN bodegas b ON b.id_bodega = ex.id_bodega
WHERE m.nombre = 'Cable DMX 5 pines 10 m' AND b.nombre = 'Bodega 2' AND ex.cantidad >= 5;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM venta_prueba) THEN
        RAISE EXCEPTION 'No hay existencia suficiente de cables DMX en Bodega 2 para la venta de prueba';
    END IF;
END $$;

WITH venta AS (
    INSERT INTO movimientos_financieros (id_tipo_movimiento, concepto, monto, fecha_movimiento, numero_factura,
                                         estado, creado_por, fecha_creacion)
    SELECT t.id_tipo_movimiento, 'Venta de cables DMX usados al Teatro Municipal', 1250.00, DATE '2026-08-20',
           '000469', 'REGISTRADO', ref.admin, TIMESTAMP '2026-08-20 11:00'
    FROM ref, tipos_movimiento t WHERE t.nombre = 'Venta de equipos'
    RETURNING id_movimiento
)
INSERT INTO ventas_detalle (id_movimiento, id_existencia, cantidad)
SELECT venta.id_movimiento, v.id_existencia, v.cantidad FROM venta, venta_prueba v;

UPDATE existencias_lote ex SET cantidad = ex.cantidad - v.cantidad
FROM venta_prueba v WHERE ex.id_existencia = v.id_existencia;

INSERT INTO movimientos_lote (id_existencia, tipo, cantidad, motivo, fecha, creado_por)
SELECT v.id_existencia, 'VENTA', -v.cantidad,
       'Movimiento financiero N.º ' || m.id_movimiento || ' (Venta de cables DMX usados al Teatro Municipal)',
       TIMESTAMP '2026-08-20 11:00', ref.admin
FROM ref, venta_prueba v, movimientos_financieros m
WHERE m.concepto = 'Venta de cables DMX usados al Teatro Municipal';

-- Costos de los mantenimientos finalizados (la aplicacion los registra al finalizar)
INSERT INTO movimientos_financieros (id_tipo_movimiento, concepto, monto, fecha_movimiento, id_mantenimiento,
                                     creado_por, fecha_creacion)
SELECT t.id_tipo_movimiento, 'Mantenimiento ' || LOWER(tm.nombre) || ' · ' || a.codigo_inventario,
       m.costo, m.fecha_fin::DATE, m.id_mantenimiento, m.modificado_por, COALESCE(m.fecha_modificacion, m.fecha_fin)
FROM mantenimientos m
JOIN tipos_mantenimiento tm ON tm.id_tipo_mantenimiento = m.id_tipo_mantenimiento
JOIN activos a ON a.id_activo = m.id_activo
JOIN tipos_movimiento t ON t.codigo_sistema = 'MANTENIMIENTO'
WHERE m.estado = 'FINALIZADO' AND m.fecha_eliminacion IS NULL AND m.costo > 0
ON CONFLICT (id_mantenimiento) DO NOTHING;

COMMIT;
