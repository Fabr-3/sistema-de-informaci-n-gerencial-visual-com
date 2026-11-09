-- =====================================================================
-- Datos de prueba: modulo de logistica (personal tecnico, jornadas de
-- montaje y desmontaje y asignaciones de personal).
-- Requiere la migracion V7 y los scripts 01 y 02 cargados.
-- Uso:
--   psql -h localhost -U postgres -d proyecto-visual-com -f database/datos_prueba/03_logistica.sql
-- Incluye un cruce intencional: Daniela Gutierrez esta asignada el 11/12
-- al montaje de pantallas e iluminacion y, a la vez, a las pruebas de la
-- cena de gala (el sistema lo alerta sin bloquearlo).
-- =====================================================================
\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM personal_tecnico) THEN
        RAISE EXCEPTION 'Los datos de prueba de logistica ya fueron cargados';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM eventos WHERE nombre_evento = 'Cena de gala de fin de año') THEN
        RAISE EXCEPTION 'Cargue primero 02_comercial.sql';
    END IF;
END $$;

CREATE TEMP TABLE ref ON COMMIT DROP AS
SELECT (SELECT id_usuario FROM usuarios WHERE nombre_usuario = 'admin')   AS admin,
       (SELECT id_usuario FROM usuarios WHERE nombre_usuario = 'mvargas') AS gerente,
       (SELECT id_tipo_documento FROM tipos_documento WHERE nombre = 'Carnet de identidad') AS ci;

-- ---------------------------------------------------------------------
-- Personal tecnico (con cuenta del sistema los tecnicos y sub-contratados)
-- ---------------------------------------------------------------------
CREATE TEMP TABLE personal_prueba ON COMMIT DROP AS
SELECT * FROM (VALUES
    ('Javier',        'Mamani Choque',    '71234501', TRUE,  'jmamani',    '4876512 LP'),
    ('Rodrigo',       'Condori Flores',   '71234502', TRUE,  'rcondori',   '5123987 LP'),
    ('Daniela',       'Gutiérrez Arce',   '71234503', TRUE,  'dgutierrez', '6034521 LP'),
    ('Fernando',      'Torrez Aliaga',    '72045510', TRUE,  'ftorrez',    '7012456 LP'),
    ('Carla',         'Mendoza',          '76598120', FALSE, 'cmendoza',   '6890123 LP'),
    ('Wilber',        'Choque Ticona',    '73011245', TRUE,  NULL,         '4455667 LP'),
    ('Freddy',        'Huanca Mamani',    '70612398', TRUE,  NULL,         '5566778 LP'),
    ('Marco Antonio', 'Limachi Paz',      '77280034', TRUE,  NULL,         '8123456 LP'),
    ('Gabriela',      'Rojas Vargas',     '76001922', TRUE,  NULL,         '7345612 SC')
) AS p(nombres, apellidos, telefono, activo, usuario, ci);

INSERT INTO personal_tecnico (nombres, apellidos, telefono, activo, id_usuario, creado_por, fecha_creacion)
SELECT p.nombres, p.apellidos, p.telefono, p.activo, u.id_usuario, ref.admin, TIMESTAMP '2026-02-02 09:00'
FROM ref, personal_prueba p
LEFT JOIN usuarios u ON u.nombre_usuario = p.usuario;

INSERT INTO documentos_personal (id_personal, id_tipo_documento, numero, creado_por, fecha_creacion)
SELECT pt.id_personal, ref.ci, p.ci, ref.admin, TIMESTAMP '2026-02-02 09:00'
FROM ref, personal_prueba p
JOIN personal_tecnico pt ON pt.nombres = p.nombres AND pt.apellidos = p.apellidos;

INSERT INTO personal_especialidades (id_personal, id_especialidad)
SELECT pt.id_personal, e.id_especialidad
FROM (VALUES
    ('Javier',        'Pantallas LED y video'),
    ('Javier',        'Estructuras y rigging'),
    ('Rodrigo',       'Sonido'),
    ('Rodrigo',       'Electricidad y generación'),
    ('Daniela',       'Iluminación'),
    ('Daniela',       'Electricidad y generación'),
    ('Fernando',      'Pantallas LED y video'),
    ('Carla',         'Iluminación'),
    ('Wilber',        'Transporte'),
    ('Freddy',        'Estructuras y rigging'),
    ('Gabriela',      'Iluminación')
) AS x(nombres, especialidad)
JOIN personal_tecnico pt ON pt.nombres = x.nombres
JOIN especialidades_tecnicas e ON e.nombre = x.especialidad;

-- ---------------------------------------------------------------------
-- Planes logisticos (los pasados por el administrador, los proximos por
-- la gerencia)
-- ---------------------------------------------------------------------
INSERT INTO planes_logisticos (id_evento, detalles_transporte, creado_por, fecha_creacion)
SELECT e.id_evento, x.transporte, CASE WHEN x.gerencia THEN ref.gerente ELSE ref.admin END, x.creado
FROM ref, (VALUES
    ('Montaje de pantalla y sonido',
     'Camión 1 sale de Bodega 1 a las 12:30 con módulos LED, procesador y sonido. Descarga por el ingreso de servicio del hotel.',
     FALSE, TIMESTAMP '2026-06-10 10:00'),
    ('Congreso Nacional de Banca',
     'Retiro con el camión 1 al cierre del congreso; los equipos vuelven a Bodega 1 la misma noche.',
     FALSE, TIMESTAMP '2026-06-10 10:15'),
    ('Concierto aniversario 25 años',
     'Dos camiones desde Bodega 1 (sonido line array y torres de iluminación). Retorno al amanecer del 17/07.',
     FALSE, TIMESTAMP '2026-07-08 16:00'),
    ('Feria Internacional FEXPOCRUZ 2026',
     'Equipos enviados a Santa Cruz por transporte de carga el 14/09; el retorno a La Paz sale el 29/09.',
     FALSE, TIMESTAMP '2026-09-05 11:00'),
    ('Lanzamiento de línea automotriz 2027',
     'Camión 1 con el piso LED y la pantalla de fondo; vagoneta para cables y accesorios.',
     FALSE, TIMESTAMP '2026-10-01 09:30'),
    ('Montaje de pantallas e iluminación',
     'Camión 1 sale de Bodega 1 a las 13:00 con pantallas interiores y torres de iluminación.',
     TRUE, TIMESTAMP '2026-11-04 15:00'),
    ('Cena de gala de fin de año',
     'Camión 2 para el retiro: sale de Bodega 2 a las 00:00. El salón debe quedar libre a las 06:00.',
     TRUE, TIMESTAMP '2026-11-04 15:20'),
    ('Montaje en el palco',
     'Viaje a Oruro el 12/02 a las 05:00 en el camión 1 y la vagoneta. Hospedaje en el Hotel Edén.',
     TRUE, TIMESTAMP '2026-11-05 10:00'),
    ('Entrada del Carnaval · sábado',
     'El equipo permanece en el palco; la vagoneta traslada al personal desde el hotel a las 06:15.',
     TRUE, TIMESTAMP '2026-11-05 10:10'),
    ('Entrada del Carnaval · domingo',
     'Retiro de pantallas al cierre de la entrada y retorno a La Paz el 15/02 a las 06:00.',
     TRUE, TIMESTAMP '2026-11-05 10:20')
) AS x(evento, transporte, gerencia, creado)
JOIN eventos e ON e.nombre_evento = x.evento;

-- ---------------------------------------------------------------------
-- Jornadas: el montaje termina antes del evento y el desmontaje empieza
-- despues
-- ---------------------------------------------------------------------
INSERT INTO jornadas_logisticas (id_plan, tipo, fecha_inicio, fecha_fin, descripcion, creado_por, fecha_creacion)
SELECT pl.id_plan, x.tipo, x.inicio, x.fin, x.descripcion, pl.creado_por, pl.fecha_creacion
FROM (VALUES
    ('Congreso Nacional de Banca', 'DESMONTAJE',
     TIMESTAMP '2026-06-18 18:30', TIMESTAMP '2026-06-18 22:30', 'Retiro de la pantalla central y del sonido'),
    ('Concierto aniversario 25 años', 'DESMONTAJE',
     TIMESTAMP '2026-07-17 00:30', TIMESTAMP '2026-07-17 06:00', 'Desarme de escenario, line array e iluminación'),
    ('Feria Internacional FEXPOCRUZ 2026', 'DESMONTAJE',
     TIMESTAMP '2026-09-27 21:30', TIMESTAMP '2026-09-28 03:00', 'Desarme del stand y embalaje para el envío'),
    ('Lanzamiento de línea automotriz 2027', 'MONTAJE',
     TIMESTAMP '2026-10-09 08:00', TIMESTAMP '2026-10-09 15:30', 'Piso LED, pantalla de fondo y pruebas'),
    ('Lanzamiento de línea automotriz 2027', 'DESMONTAJE',
     TIMESTAMP '2026-10-10 00:00', TIMESTAMP '2026-10-10 04:00', 'Retiro del piso LED'),
    ('Cena de gala de fin de año', 'MONTAJE',
     TIMESTAMP '2026-12-11 18:00', TIMESTAMP '2026-12-11 22:00', 'Pruebas de iluminación con el salón a oscuras'),
    ('Cena de gala de fin de año', 'MONTAJE',
     TIMESTAMP '2026-12-12 09:00', TIMESTAMP '2026-12-12 17:00', 'Ajuste final de pantallas, iluminación y prueba de sonido'),
    ('Cena de gala de fin de año', 'DESMONTAJE',
     TIMESTAMP '2026-12-13 00:30', TIMESTAMP '2026-12-13 05:00', 'Retiro total de equipos'),
    ('Entrada del Carnaval · domingo', 'DESMONTAJE',
     TIMESTAMP '2027-02-14 22:30', TIMESTAMP '2027-02-15 04:00', 'Retiro de pantallas del palco')
) AS x(evento, tipo, inicio, fin, descripcion)
JOIN eventos e ON e.nombre_evento = x.evento
JOIN planes_logisticos pl ON pl.id_evento = e.id_evento;

-- ---------------------------------------------------------------------
-- Asignaciones de personal (dentro de la ventana de cada plan)
-- ---------------------------------------------------------------------
INSERT INTO asignaciones_personal (id_plan, id_personal, id_rol_evento, fecha_inicio_actividad, fecha_fin_actividad,
                                   creado_por, fecha_creacion)
SELECT pl.id_plan, pt.id_personal, r.id_rol_evento, x.inicio, x.fin, pl.creado_por, pl.fecha_creacion
FROM (VALUES
    ('Montaje de pantalla y sonido', 'Javier', 'Jefe de montaje',
     TIMESTAMP '2026-06-17 14:00', TIMESTAMP '2026-06-17 22:00'),
    ('Montaje de pantalla y sonido', 'Freddy', 'Rigger',
     TIMESTAMP '2026-06-17 14:00', TIMESTAMP '2026-06-17 20:00'),
    ('Montaje de pantalla y sonido', 'Wilber', 'Chofer',
     TIMESTAMP '2026-06-17 14:00', TIMESTAMP '2026-06-17 15:00'),
    ('Congreso Nacional de Banca', 'Javier', 'Técnico de pantallas LED',
     TIMESTAMP '2026-06-18 07:00', TIMESTAMP '2026-06-18 22:30'),
    ('Congreso Nacional de Banca', 'Rodrigo', 'Técnico de sonido',
     TIMESTAMP '2026-06-18 07:00', TIMESTAMP '2026-06-18 18:00'),
    ('Congreso Nacional de Banca', 'Freddy', 'Rigger',
     TIMESTAMP '2026-06-18 18:30', TIMESTAMP '2026-06-18 22:30'),
    ('Congreso Nacional de Banca', 'Wilber', 'Chofer',
     TIMESTAMP '2026-06-18 20:30', TIMESTAMP '2026-06-18 22:30'),
    ('Concierto aniversario 25 años', 'Rodrigo', 'Técnico de sonido',
     TIMESTAMP '2026-07-16 15:00', TIMESTAMP '2026-07-16 23:45'),
    ('Concierto aniversario 25 años', 'Daniela', 'Técnico de iluminación',
     TIMESTAMP '2026-07-16 15:00', TIMESTAMP '2026-07-16 23:45'),
    ('Concierto aniversario 25 años', 'Carla', 'Técnico de iluminación',
     TIMESTAMP '2026-07-16 18:00', TIMESTAMP '2026-07-16 23:45'),
    ('Concierto aniversario 25 años', 'Fernando', 'Operador de video',
     TIMESTAMP '2026-07-16 15:00', TIMESTAMP '2026-07-16 23:45'),
    ('Concierto aniversario 25 años', 'Freddy', 'Rigger',
     TIMESTAMP '2026-07-17 00:30', TIMESTAMP '2026-07-17 06:00'),
    ('Concierto aniversario 25 años', 'Marco Antonio', 'Ayudante de montaje',
     TIMESTAMP '2026-07-17 00:30', TIMESTAMP '2026-07-17 06:00'),
    ('Concierto aniversario 25 años', 'Wilber', 'Chofer',
     TIMESTAMP '2026-07-17 04:00', TIMESTAMP '2026-07-17 06:00'),
    ('Feria Internacional FEXPOCRUZ 2026', 'Fernando', 'Técnico de pantallas LED',
     TIMESTAMP '2026-09-18 09:00', TIMESTAMP '2026-09-27 21:00'),
    ('Feria Internacional FEXPOCRUZ 2026', 'Gabriela', 'Técnico de iluminación',
     TIMESTAMP '2026-09-27 21:30', TIMESTAMP '2026-09-28 03:00'),
    ('Lanzamiento de línea automotriz 2027', 'Javier', 'Jefe de montaje',
     TIMESTAMP '2026-10-09 08:00', TIMESTAMP '2026-10-09 23:30'),
    ('Lanzamiento de línea automotriz 2027', 'Fernando', 'Técnico de pantallas LED',
     TIMESTAMP '2026-10-09 08:00', TIMESTAMP '2026-10-09 23:30'),
    ('Lanzamiento de línea automotriz 2027', 'Marco Antonio', 'Ayudante de montaje',
     TIMESTAMP '2026-10-09 08:00', TIMESTAMP '2026-10-09 15:30'),
    ('Lanzamiento de línea automotriz 2027', 'Marco Antonio', 'Ayudante de montaje',
     TIMESTAMP '2026-10-10 00:00', TIMESTAMP '2026-10-10 04:00'),
    ('Lanzamiento de línea automotriz 2027', 'Wilber', 'Chofer',
     TIMESTAMP '2026-10-10 02:00', TIMESTAMP '2026-10-10 04:00'),
    ('Montaje de pantallas e iluminación', 'Javier', 'Jefe de montaje',
     TIMESTAMP '2026-12-11 14:00', TIMESTAMP '2026-12-11 22:00'),
    ('Montaje de pantallas e iluminación', 'Daniela', 'Técnico de iluminación',
     TIMESTAMP '2026-12-11 14:00', TIMESTAMP '2026-12-11 22:00'),
    ('Montaje de pantallas e iluminación', 'Freddy', 'Rigger',
     TIMESTAMP '2026-12-11 14:00', TIMESTAMP '2026-12-11 20:00'),
    ('Cena de gala de fin de año', 'Daniela', 'Técnico de iluminación',
     TIMESTAMP '2026-12-11 18:00', TIMESTAMP '2026-12-11 22:00'),
    ('Cena de gala de fin de año', 'Daniela', 'Técnico de iluminación',
     TIMESTAMP '2026-12-12 09:00', TIMESTAMP '2026-12-12 23:59'),
    ('Cena de gala de fin de año', 'Javier', 'Técnico de pantallas LED',
     TIMESTAMP '2026-12-12 09:00', TIMESTAMP '2026-12-12 23:59'),
    ('Cena de gala de fin de año', 'Rodrigo', 'Técnico de sonido',
     TIMESTAMP '2026-12-12 15:00', TIMESTAMP '2026-12-12 23:59'),
    ('Cena de gala de fin de año', 'Freddy', 'Rigger',
     TIMESTAMP '2026-12-13 00:30', TIMESTAMP '2026-12-13 05:00'),
    ('Cena de gala de fin de año', 'Marco Antonio', 'Ayudante de montaje',
     TIMESTAMP '2026-12-13 00:30', TIMESTAMP '2026-12-13 05:00'),
    ('Cena de gala de fin de año', 'Wilber', 'Chofer',
     TIMESTAMP '2026-12-13 00:00', TIMESTAMP '2026-12-13 05:00'),
    ('Montaje en el palco', 'Javier', 'Jefe de montaje',
     TIMESTAMP '2027-02-12 08:00', TIMESTAMP '2027-02-12 20:00'),
    ('Montaje en el palco', 'Freddy', 'Rigger',
     TIMESTAMP '2027-02-12 08:00', TIMESTAMP '2027-02-12 20:00'),
    ('Montaje en el palco', 'Wilber', 'Chofer',
     TIMESTAMP '2027-02-12 08:00', TIMESTAMP '2027-02-12 10:00'),
    ('Entrada del Carnaval · sábado', 'Fernando', 'Operador de video',
     TIMESTAMP '2027-02-13 07:00', TIMESTAMP '2027-02-13 23:00'),
    ('Entrada del Carnaval · sábado', 'Rodrigo', 'Técnico de sonido',
     TIMESTAMP '2027-02-13 07:00', TIMESTAMP '2027-02-13 23:00'),
    ('Entrada del Carnaval · domingo', 'Fernando', 'Operador de video',
     TIMESTAMP '2027-02-14 07:00', TIMESTAMP '2027-02-14 22:00'),
    ('Entrada del Carnaval · domingo', 'Rodrigo', 'Técnico de sonido',
     TIMESTAMP '2027-02-14 07:00', TIMESTAMP '2027-02-14 22:00'),
    ('Entrada del Carnaval · domingo', 'Freddy', 'Rigger',
     TIMESTAMP '2027-02-14 22:30', TIMESTAMP '2027-02-15 04:00'),
    ('Entrada del Carnaval · domingo', 'Wilber', 'Chofer',
     TIMESTAMP '2027-02-15 02:00', TIMESTAMP '2027-02-15 04:00')
) AS x(evento, nombres, rol, inicio, fin)
JOIN eventos e ON e.nombre_evento = x.evento
JOIN planes_logisticos pl ON pl.id_evento = e.id_evento
JOIN personal_tecnico pt ON pt.nombres = x.nombres
JOIN roles_evento r ON r.nombre = x.rol;

COMMIT;
