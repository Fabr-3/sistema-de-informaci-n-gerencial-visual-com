-- =====================================================================
-- Datos de prueba: modulo comercial y de planificacion de proyectos.
-- Requiere las migraciones aplicadas y 01_accesos_activos.sql cargado.
-- Uso:
--   psql -h localhost -U postgres -d proyecto-visual-com -f database/datos_prueba/02_comercial.sql
-- Los eventos ya ejecutados coinciden con el historial de uso de los
-- equipos cargado en 01_accesos_activos.sql. No modifica datos existentes.
-- =====================================================================
\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM cotizaciones WHERE numero = 'COT-2026-0001') THEN
        RAISE EXCEPTION 'Los datos de prueba comerciales ya fueron cargados';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM usuarios WHERE nombre_usuario = 'mvargas') THEN
        RAISE EXCEPTION 'Cargue primero 01_accesos_activos.sql';
    END IF;
END $$;

CREATE TEMP TABLE ref ON COMMIT DROP AS
SELECT (SELECT id_usuario FROM usuarios WHERE nombre_usuario = 'admin')   AS admin,
       (SELECT id_usuario FROM usuarios WHERE nombre_usuario = 'mvargas') AS gerente;

-- ---------------------------------------------------------------------
-- Servicios (precios referenciales en Bs)
-- ---------------------------------------------------------------------
INSERT INTO servicios (nombre, descripcion, unidad_medida, precio_referencial, activo, creado_por, fecha_creacion)
SELECT s.nombre, s.descripcion, s.unidad, s.precio, s.activo, ref.admin, TIMESTAMP '2026-01-20 10:00'
FROM ref, (VALUES
    ('Pantalla LED exterior',               'Pantalla modular P3.9 apta para exterior, incluye procesador', 'm² por día',           180.00,  TRUE),
    ('Pantalla LED interior',               'Pantalla de alta resolución para salones y escenarios',        'm² por día',           220.00,  TRUE),
    ('Piso LED',                            'Piso LED transitable para pasarelas y escenarios',             'm² por día',           350.00,  TRUE),
    ('Sistema de sonido line array',        'Sistema para conciertos y espacios abiertos con consola digital', 'jornada',        9500.00,  TRUE),
    ('Sonido para conferencias',            'Parlantes, consola y microfonía para auditorios',              'jornada',             2800.00,  TRUE),
    ('Iluminación escénica',                'Cabezas móviles, wash y control DMX',                          'jornada',             6500.00,  TRUE),
    ('Micrófono inalámbrico',               'Micrófono de mano inalámbrico',                                'unidad por día',       150.00,  TRUE),
    ('Operación técnica',                   'Técnico especializado durante el evento',                      'técnico por jornada',  450.00,  TRUE),
    ('Producción de contenido visual',      'Diseño y edición de contenidos para pantallas',                'paquete',             3500.00,  TRUE),
    ('Video mapping',                       'Proyección sobre fachadas o superficies irregulares',          'proyecto',           18000.00,  TRUE),
    ('Montaje de estructuras',              'Armado y desmontaje de truss y soportes',                      'servicio',            2500.00,  TRUE),
    ('Transporte urbano de equipos',        'La Paz y El Alto',                                             'viaje',                900.00,  TRUE),
    ('Transporte interdepartamental',       'Traslado de equipos entre departamentos',                      'viaje',               4800.00,  TRUE),
    ('Proyector 10 000 lúmenes',            'Proyector láser con pantalla de proyección',                   'día',                 1200.00,  FALSE)
) AS s(nombre, descripcion, unidad, precio, activo);

-- ---------------------------------------------------------------------
-- Lugares
-- ---------------------------------------------------------------------
INSERT INTO lugares (nombre, direccion, ciudad, creado_por, fecha_creacion)
SELECT l.nombre, l.direccion, l.ciudad, ref.admin, TIMESTAMP '2026-01-20 10:30'
FROM ref, (VALUES
    ('Hotel Casa Grande · Salón Illimani',   'Av. Ballivián 958, Calacoto',                   'La Paz'),
    ('Teatro al Aire Libre Jaime Laredo',    'Av. del Ejército, Miraflores',                  'La Paz'),
    ('Coliseo Cerrado Julio Borelli',        'Av. Saavedra, Miraflores',                      'La Paz'),
    ('Megacenter Irpavi · explanada',        'Av. Rafael Pabón, Irpavi',                      'La Paz'),
    ('Recinto Ferial Fexpocruz',             'Av. Roca y Coronado, 3.er anillo',              'Santa Cruz de la Sierra'),
    ('Palco oficial Av. Montes',             'Av. Montes esq. Uruguay',                       'La Paz'),
    ('Salón de eventos Los Arcos',           'Calle 21 de Calacoto 8340',                     'La Paz'),
    ('Av. 6 de Agosto · palco Carnaval',     'Av. 6 de Agosto esq. Junín',                    'Oruro')
) AS l(nombre, direccion, ciudad);

-- ---------------------------------------------------------------------
-- Clientes con documentos y contactos
-- ---------------------------------------------------------------------
INSERT INTO clientes (id_tipo_cliente, razon_social, direccion, creado_por, fecha_creacion)
SELECT (SELECT id_tipo_cliente FROM tipos_cliente WHERE nombre = c.tipo), c.razon, c.direccion, ref.admin, c.alta
FROM ref, (VALUES
    ('Empresa privada',     'Banco Andino de Desarrollo S.A.',            'Av. Camacho 1277, piso 3, La Paz',            TIMESTAMP '2026-02-03 09:00'),
    ('Empresa privada',     'Cervecería del Altiplano S.R.L.',            'Av. Montes 400, La Paz',                      TIMESTAMP '2026-02-10 11:00'),
    ('Empresa privada',     'Automotores Illimani S.A.',                  'Av. Hernando Siles 5200, Obrajes, La Paz',    TIMESTAMP '2026-03-05 15:00'),
    ('Empresa privada',     'Productora Sonido Libre S.R.L.',             'Calle Jaén 711, La Paz',                      TIMESTAMP '2026-04-14 10:00'),
    ('Empresa privada',     'Asociación de Exportadores del Sur',         'Av. San Martín 1800, Equipetrol, Santa Cruz', TIMESTAMP '2026-05-20 09:30'),
    ('Institución pública', 'Instituto Tecnológico Superior Andino',      'Av. Juan Pablo II 2560, El Alto',             TIMESTAMP '2026-08-11 08:30'),
    ('Persona natural',     'María Fernanda Salazar Rojas',               'Calle 15 de Calacoto 7890, La Paz',           TIMESTAMP '2026-08-25 18:00'),
    ('Empresa privada',     'Fraternidad Folklórica Señorial Illimani',   'Calle Max Paredes 912, La Paz',               TIMESTAMP '2026-10-02 19:00')
) AS c(tipo, razon, direccion, alta);

INSERT INTO documentos_cliente (id_cliente, id_tipo_documento, numero, creado_por, fecha_creacion)
SELECT cl.id_cliente, (SELECT id_tipo_documento FROM tipos_documento WHERE nombre = d.tipo), d.numero, ref.admin,
       cl.fecha_creacion
FROM ref, (VALUES
    ('Banco Andino de Desarrollo S.A.',          'NIT',                 '1020304025'),
    ('Cervecería del Altiplano S.R.L.',          'NIT',                 '1028847021'),
    ('Automotores Illimani S.A.',                'NIT',                 '1007659023'),
    ('Productora Sonido Libre S.R.L.',           'NIT',                 '352619014'),
    ('Asociación de Exportadores del Sur',       'NIT',                 '178452026'),
    ('Instituto Tecnológico Superior Andino',    'NIT',                 '305477018'),
    ('María Fernanda Salazar Rojas',             'Carnet de identidad', '6845123 LP'),
    ('Fraternidad Folklórica Señorial Illimani', 'NIT',                 '411029015'),
    ('Fraternidad Folklórica Señorial Illimani', 'Carnet de identidad', '4512870 LP')
) AS d(cliente, tipo, numero)
JOIN clientes cl ON cl.razon_social = d.cliente;

INSERT INTO contactos_cliente (id_cliente, id_tipo_contacto, valor, nombre_referencia)
SELECT cl.id_cliente, (SELECT id_tipo_contacto FROM tipos_contacto WHERE nombre = c.tipo), c.valor, c.referencia
FROM (VALUES
    ('Banco Andino de Desarrollo S.A.',          'Correo electrónico', 'eventos@bancoandino.com.bo',      'Gerencia de Marketing'),
    ('Banco Andino de Desarrollo S.A.',          'Teléfono fijo',      '2 2443300 int. 210',              'Gerencia de Marketing'),
    ('Cervecería del Altiplano S.R.L.',          'Celular',            '71524488',                        'Jefatura de Marca'),
    ('Cervecería del Altiplano S.R.L.',          'Correo electrónico', 'marca@cerveceriaaltiplano.com.bo', 'Jefatura de Marca'),
    ('Automotores Illimani S.A.',                'Celular',            '72039915',                        'Coordinación comercial'),
    ('Productora Sonido Libre S.R.L.',           'Celular',            '76781230',                        'Producción'),
    ('Asociación de Exportadores del Sur',       'Correo electrónico', 'ferias@exportadoresdelsur.org.bo', 'Comité de ferias'),
    ('Instituto Tecnológico Superior Andino',    'Teléfono fijo',      '2 2851120',                       'Dirección administrativa'),
    ('María Fernanda Salazar Rojas',             'Celular',            '70145562',                        NULL),
    ('Fraternidad Folklórica Señorial Illimani', 'Celular',            '73004521',                        'Pasante 2027')
) AS c(cliente, tipo, valor, referencia)
JOIN clientes cl ON cl.razon_social = c.cliente;

-- ---------------------------------------------------------------------
-- Cotizaciones (se registran en borrador y luego se resuelven, porque el
-- trigger solo admite lineas en cotizaciones en borrador)
-- ---------------------------------------------------------------------
CREATE TEMP TABLE cotizaciones_prueba (
    numero TEXT, cliente TEXT, titulo TEXT, emision DATE, validez DATE, evento_inicio DATE, evento_fin DATE,
    estado TEXT, resolucion TIMESTAMP, motivo TEXT, condiciones TEXT
) ON COMMIT DROP;
INSERT INTO cotizaciones_prueba VALUES
    ('COT-2026-0001', 'Banco Andino de Desarrollo S.A.', 'Congreso Nacional de Banca 2026 · pantallas y sonido',
     '2026-05-12', '2026-05-31', '2026-06-18', '2026-06-18', 'ACEPTADA', '2026-05-20 16:30', NULL,
     'Incluye montaje el día previo. Anticipo del 50 % a la firma del contrato.'),
    ('COT-2026-0002', 'Cervecería del Altiplano S.R.L.', 'Concierto aniversario 25 años · producción técnica integral',
     '2026-06-02', '2026-06-20', '2026-07-16', '2026-07-16', 'ACEPTADA', '2026-06-10 11:00', NULL,
     'El cliente gestiona los permisos municipales y la energía del recinto.'),
    ('COT-2026-0003', 'Asociación de Exportadores del Sur', 'Stand corporativo FEXPOCRUZ 2026',
     '2026-07-28', '2026-08-15', '2026-09-18', '2026-09-27', 'ACEPTADA', '2026-08-05 10:15', NULL,
     'Precios por los 10 días de feria. Transporte ida y vuelta La Paz – Santa Cruz.'),
    ('COT-2026-0004', 'Automotores Illimani S.A.', 'Lanzamiento de línea automotriz 2027',
     '2026-08-20', '2026-09-10', '2026-10-09', '2026-10-09', 'ACEPTADA', '2026-09-02 17:45', NULL, NULL),
    ('COT-2026-0005', 'Productora Sonido Libre S.R.L.', 'Festival universitario de bandas · sonido y pantallas',
     '2026-09-15', '2026-10-05', '2026-10-24', '2026-10-24', 'ACEPTADA', '2026-09-28 09:20', NULL, NULL),
    ('COT-2026-0006', 'María Fernanda Salazar Rojas', 'Boda Salazar – Gutiérrez · iluminación y sonido',
     '2026-08-26', '2026-09-15', '2026-10-17', '2026-10-17', 'RECHAZADA', '2026-09-09 12:00',
     'La clienta contrató un paquete integral ofrecido por el salón de eventos', NULL),
    ('COT-2026-0007', 'Instituto Tecnológico Superior Andino', 'Acto de graduación de promociones 2026',
     '2026-09-29', '2026-10-20', '2026-11-27', '2026-11-27', 'VENCIDA', NULL, NULL,
     'Sujeto a la emisión de la orden de compra institucional.'),
    ('COT-2026-0008', 'Banco Andino de Desarrollo S.A.', 'Cena de gala de fin de año',
     '2026-10-14', '2026-11-05', '2026-12-11', '2026-12-12', 'ACEPTADA', '2026-10-30 15:00', NULL,
     'Montaje el 11 de diciembre desde las 14:00.'),
    ('COT-2026-0009', 'Cervecería del Altiplano S.R.L.', 'Activación de marca · Carnaval de Oruro 2027',
     '2026-10-20', '2026-11-20', '2027-02-13', '2027-02-14', 'ACEPTADA', '2026-11-04 10:00', NULL,
     'Incluye viáticos del personal técnico durante los dos días de entrada.'),
    ('COT-2026-0010', 'Fraternidad Folklórica Señorial Illimani', 'Pantallas para la Entrada del Gran Poder 2027',
     '2026-11-03', '2026-12-15', '2027-06-05', '2027-06-05', 'BORRADOR', NULL, NULL,
     'Precio sujeto a la ubicación final del palco asignado por la Asociación.'),
    ('COT-2026-0011', 'Asociación de Exportadores del Sur', 'Rueda de negocios de exportadores · auditorio',
     '2026-11-06', '2026-11-25', '2026-12-03', '2026-12-03', 'BORRADOR', NULL, NULL, NULL);

INSERT INTO cotizaciones (numero, id_cliente, fecha_emision, estado, condiciones_comerciales, titulo, fecha_validez,
                          fecha_evento_inicio, fecha_evento_fin, creado_por, fecha_creacion)
SELECT c.numero, cl.id_cliente, c.emision, 'BORRADOR', c.condiciones, c.titulo, c.validez, c.evento_inicio,
       c.evento_fin, ref.admin, c.emision + TIME '10:00'
FROM ref, cotizaciones_prueba c
JOIN clientes cl ON cl.razon_social = c.cliente;

INSERT INTO cotizacion_servicios (id_cotizacion, id_servicio, cantidad, precio_unitario, descuento)
SELECT co.id_cotizacion, s.id_servicio, l.cantidad, l.precio, l.descuento
FROM (VALUES
    ('COT-2026-0001', 'Pantalla LED interior',          24, 220.00,    0.00),
    ('COT-2026-0001', 'Sonido para conferencias',        1, 2800.00,   0.00),
    ('COT-2026-0001', 'Micrófono inalámbrico',           8, 150.00,    0.00),
    ('COT-2026-0001', 'Operación técnica',               3, 450.00,    0.00),
    ('COT-2026-0001', 'Transporte urbano de equipos',    2, 900.00,  300.00),
    ('COT-2026-0002', 'Pantalla LED exterior',          54, 180.00,  720.00),
    ('COT-2026-0002', 'Sistema de sonido line array',    1, 9500.00,   0.00),
    ('COT-2026-0002', 'Iluminación escénica',            1, 6500.00,   0.00),
    ('COT-2026-0002', 'Montaje de estructuras',          1, 2500.00,   0.00),
    ('COT-2026-0002', 'Operación técnica',               6, 450.00,    0.00),
    ('COT-2026-0002', 'Transporte urbano de equipos',    3, 900.00,    0.00),
    ('COT-2026-0003', 'Pantalla LED interior',         120, 220.00, 2400.00),
    ('COT-2026-0003', 'Piso LED',                       40, 350.00,    0.00),
    ('COT-2026-0003', 'Producción de contenido visual',  1, 3500.00,   0.00),
    ('COT-2026-0003', 'Operación técnica',              10, 450.00,    0.00),
    ('COT-2026-0003', 'Transporte interdepartamental',   2, 4800.00,   0.00),
    ('COT-2026-0004', 'Piso LED',                        8, 350.00,    0.00),
    ('COT-2026-0004', 'Iluminación escénica',            1, 6500.00, 500.00),
    ('COT-2026-0004', 'Micrófono inalámbrico',           4, 150.00,    0.00),
    ('COT-2026-0004', 'Producción de contenido visual',  1, 3500.00,   0.00),
    ('COT-2026-0004', 'Operación técnica',               3, 450.00,    0.00),
    ('COT-2026-0005', 'Pantalla LED exterior',          36, 180.00,    0.00),
    ('COT-2026-0005', 'Sistema de sonido line array',    1, 9500.00, 1000.00),
    ('COT-2026-0005', 'Iluminación escénica',            1, 6500.00,   0.00),
    ('COT-2026-0005', 'Operación técnica',               5, 450.00,    0.00),
    ('COT-2026-0006', 'Iluminación escénica',            1, 6500.00,   0.00),
    ('COT-2026-0006', 'Sonido para conferencias',        1, 2800.00,   0.00),
    ('COT-2026-0007', 'Pantalla LED interior',          16, 220.00,    0.00),
    ('COT-2026-0007', 'Sonido para conferencias',        1, 2800.00,   0.00),
    ('COT-2026-0007', 'Micrófono inalámbrico',           4, 150.00,    0.00),
    ('COT-2026-0008', 'Pantalla LED interior',          24, 220.00,  480.00),
    ('COT-2026-0008', 'Iluminación escénica',            1, 6500.00,   0.00),
    ('COT-2026-0008', 'Sonido para conferencias',        2, 2800.00,   0.00),
    ('COT-2026-0008', 'Operación técnica',               4, 450.00,    0.00),
    ('COT-2026-0009', 'Pantalla LED exterior',          32, 180.00,    0.00),
    ('COT-2026-0009', 'Sistema de sonido line array',    2, 9500.00,   0.00),
    ('COT-2026-0009', 'Operación técnica',              10, 450.00,    0.00),
    ('COT-2026-0009', 'Transporte interdepartamental',   2, 4800.00,   0.00),
    ('COT-2026-0010', 'Pantalla LED exterior',          48, 180.00,    0.00),
    ('COT-2026-0010', 'Operación técnica',               4, 450.00,    0.00),
    ('COT-2026-0010', 'Montaje de estructuras',          1, 2500.00,   0.00),
    ('COT-2026-0011', 'Sonido para conferencias',        1, 2800.00,   0.00),
    ('COT-2026-0011', 'Pantalla LED interior',          12, 220.00,    0.00)
) AS l(numero, servicio, cantidad, precio, descuento)
JOIN cotizaciones co ON co.numero = l.numero
JOIN servicios s ON s.nombre = l.servicio;

UPDATE cotizaciones co
SET estado = c.estado, fecha_resolucion = c.resolucion, motivo_rechazo = c.motivo,
    resuelto_por = CASE WHEN c.resolucion IS NULL THEN NULL ELSE ref.gerente END,
    modificado_por = CASE WHEN c.resolucion IS NULL THEN NULL ELSE ref.gerente END,
    fecha_modificacion = c.resolucion
FROM ref, cotizaciones_prueba c
WHERE co.numero = c.numero AND c.estado <> 'BORRADOR';

-- ---------------------------------------------------------------------
-- Proyectos de las cotizaciones aceptadas
-- ---------------------------------------------------------------------
INSERT INTO proyectos (id_cotizacion, nombre_proyecto, descripcion, fecha_inicio, fecha_fin, estado, creado_por,
                       fecha_creacion)
SELECT co.id_cotizacion, co.titulo, p.descripcion, co.fecha_evento_inicio - p.dias_montaje, co.fecha_evento_fin,
       p.estado, ref.gerente, co.fecha_resolucion
FROM ref, (VALUES
    ('COT-2026-0001', 'FINALIZADO',    1, 'Pantalla central de 6 × 4 m y sonido para 400 asistentes'),
    ('COT-2026-0002', 'FINALIZADO',    1, 'Escenario principal con pantalla de 9 × 6 m'),
    ('COT-2026-0003', 'FINALIZADO',    2, 'Stand de 300 m² en el pabellón internacional'),
    ('COT-2026-0004', 'FINALIZADO',    0, 'Presentación de tres modelos con piso LED y show de luces'),
    ('COT-2026-0005', 'FINALIZADO',    0, NULL),
    ('COT-2026-0008', 'PLANIFICACION', 0, 'Gala para 250 invitados'),
    ('COT-2026-0009', 'PLANIFICACION', 1, 'Pantallas en el palco de la marca durante la entrada')
) AS p(numero, estado, dias_montaje, descripcion)
JOIN cotizaciones co ON co.numero = p.numero;

-- ---------------------------------------------------------------------
-- Cronograma de eventos
-- ---------------------------------------------------------------------
INSERT INTO eventos (id_proyecto, id_lugar, nombre_evento, fecha_inicio, fecha_fin, estado, creado_por, fecha_creacion)
SELECT pr.id_proyecto, lu.id_lugar, e.nombre, e.inicio, e.fin, e.estado, ref.admin, co.fecha_resolucion
FROM ref, (VALUES
    ('COT-2026-0001', 'Hotel Casa Grande · Salón Illimani', 'Montaje de pantalla y sonido',          TIMESTAMP '2026-06-17 14:00', TIMESTAMP '2026-06-17 22:00', 'EJECUTADO'),
    ('COT-2026-0001', 'Hotel Casa Grande · Salón Illimani', 'Congreso Nacional de Banca',            TIMESTAMP '2026-06-18 07:00', TIMESTAMP '2026-06-18 18:00', 'EJECUTADO'),
    ('COT-2026-0002', 'Teatro al Aire Libre Jaime Laredo',  'Montaje de escenario',                  TIMESTAMP '2026-07-15 08:00', TIMESTAMP '2026-07-15 23:00', 'EJECUTADO'),
    ('COT-2026-0002', 'Teatro al Aire Libre Jaime Laredo',  'Concierto aniversario 25 años',         TIMESTAMP '2026-07-16 15:00', TIMESTAMP '2026-07-16 23:45', 'EJECUTADO'),
    ('COT-2026-0003', 'Recinto Ferial Fexpocruz',           'Montaje del stand',                     TIMESTAMP '2026-09-16 08:00', TIMESTAMP '2026-09-17 20:00', 'EJECUTADO'),
    ('COT-2026-0003', 'Recinto Ferial Fexpocruz',           'Feria Internacional FEXPOCRUZ 2026',    TIMESTAMP '2026-09-18 09:00', TIMESTAMP '2026-09-27 21:00', 'EJECUTADO'),
    ('COT-2026-0004', 'Megacenter Irpavi · explanada',      'Lanzamiento de línea automotriz 2027',  TIMESTAMP '2026-10-09 17:00', TIMESTAMP '2026-10-09 23:30', 'EJECUTADO'),
    ('COT-2026-0005', 'Coliseo Cerrado Julio Borelli',      'Festival universitario de bandas',      TIMESTAMP '2026-10-24 16:00', TIMESTAMP '2026-10-24 23:59', 'EJECUTADO'),
    ('COT-2026-0008', 'Salón de eventos Los Arcos',         'Montaje de pantallas e iluminación',    TIMESTAMP '2026-12-11 14:00', TIMESTAMP '2026-12-11 22:00', 'PROGRAMADO'),
    ('COT-2026-0008', 'Salón de eventos Los Arcos',         'Cena de gala de fin de año',            TIMESTAMP '2026-12-12 19:00', TIMESTAMP '2026-12-12 23:59', 'PROGRAMADO'),
    ('COT-2026-0009', 'Av. 6 de Agosto · palco Carnaval',   'Montaje en el palco',                   TIMESTAMP '2027-02-12 08:00', TIMESTAMP '2027-02-12 20:00', 'PROGRAMADO'),
    ('COT-2026-0009', 'Av. 6 de Agosto · palco Carnaval',   'Entrada del Carnaval · sábado',         TIMESTAMP '2027-02-13 07:00', TIMESTAMP '2027-02-13 23:00', 'PROGRAMADO'),
    ('COT-2026-0009', 'Av. 6 de Agosto · palco Carnaval',   'Entrada del Carnaval · domingo',        TIMESTAMP '2027-02-14 07:00', TIMESTAMP '2027-02-14 22:00', 'PROGRAMADO')
) AS e(numero, lugar, nombre, inicio, fin, estado)
JOIN cotizaciones co ON co.numero = e.numero
JOIN proyectos pr ON pr.id_cotizacion = co.id_cotizacion
JOIN lugares lu ON lu.nombre = e.lugar;

-- ---------------------------------------------------------------------
-- Contratos
-- ---------------------------------------------------------------------
INSERT INTO contratos (id_proyecto, numero, fecha_firma, terminos, creado_por, fecha_creacion)
SELECT pr.id_proyecto, c.numero, c.firma, c.terminos, ref.admin, c.firma + TIME '16:00'
FROM ref, (VALUES
    ('COT-2026-0001', 'CTR-2026-0001', DATE '2026-05-25', NULL),
    ('COT-2026-0002', 'CTR-2026-0002', DATE '2026-06-15', 'El cliente provee generador de respaldo de 150 kVA.'),
    ('COT-2026-0003', 'CTR-2026-0003', DATE '2026-08-10', 'Pago del 30 % a la firma, 40 % al inicio de la feria y 30 % al cierre.'),
    ('COT-2026-0004', 'CTR-2026-0004', DATE '2026-09-08', NULL),
    ('COT-2026-0005', 'CTR-2026-0005', DATE '2026-10-01', NULL),
    ('COT-2026-0008', 'CTR-2026-0006', DATE '2026-11-05', 'Pago total contra entrega del servicio.')
) AS c(cotizacion, numero, firma, terminos)
JOIN cotizaciones co ON co.numero = c.cotizacion
JOIN proyectos pr ON pr.id_cotizacion = co.id_cotizacion;

COMMIT;
