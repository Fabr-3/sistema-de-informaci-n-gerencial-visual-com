-- =====================================================================
-- V3 - Datos iniciales del modulo de accesos.
-- Los roles y permisos son fijos: la aplicacion depende de sus codigos.
-- El usuario administrador inicial NO se crea aqui para no versionar
-- credenciales; lo crea la aplicacion al arrancar (InicializadorAdministrador).
-- =====================================================================

INSERT INTO roles (codigo, nombre, descripcion) VALUES
    ('ADMINISTRADOR',          'Administrador',           'Acceso total al sistema: gestión de usuarios, permisos, auditoría y respaldos'),
    ('GERENTE',                'Gerente',                 'Consulta de toda la información, aprobación de cotizaciones, finanzas y reportes'),
    ('TECNICO',                'Técnico',                 'Consulta de activos y asignaciones propias, registro de mantenimientos y consumos'),
    ('SUBCONTRATADO_TEMPORAL', 'Sub-contratado temporal', 'Consulta de sus asignaciones durante la vigencia de su contrato');

INSERT INTO permisos (modulo, nombre, descripcion) VALUES
    -- Administracion de accesos
    ('ACCESOS',      'USUARIOS_VER',             'Consultar usuarios'),
    ('ACCESOS',      'USUARIOS_GESTIONAR',       'Crear, editar, desactivar y eliminar usuarios'),
    ('ACCESOS',      'ROLES_VER',                'Consultar roles y sus permisos'),
    ('ACCESOS',      'ROLES_GESTIONAR',          'Modificar los permisos asignados a los roles'),
    ('ACCESOS',      'AUDITORIA_VER',            'Consultar el registro de auditoría'),
    ('ACCESOS',      'RESPALDOS_GESTIONAR',      'Generar y descargar copias de seguridad de la base de datos'),
    -- Activos e inventario
    ('ACTIVOS',      'ACTIVOS_VER',              'Consultar activos e inventario'),
    ('ACTIVOS',      'ACTIVOS_GESTIONAR',        'Registrar y modificar activos e inventario'),
    ('ACTIVOS',      'MANTENIMIENTOS_REGISTRAR', 'Registrar uso y mantenimientos de activos'),
    -- Comercial y planificacion
    ('COMERCIAL',    'COMERCIAL_VER',            'Consultar clientes, cotizaciones, contratos y cronograma'),
    ('COMERCIAL',    'COMERCIAL_GESTIONAR',      'Registrar clientes, cotizaciones, contratos y eventos'),
    ('COMERCIAL',    'COTIZACIONES_APROBAR',     'Aprobar o rechazar cotizaciones'),
    -- Logistica
    ('LOGISTICA',    'LOGISTICA_VER',            'Consultar planes logísticos y asignaciones de personal'),
    ('LOGISTICA',    'LOGISTICA_GESTIONAR',      'Planificar montaje, desmontaje y asignar personal'),
    ('LOGISTICA',    'ASIGNACIONES_VER_PROPIAS', 'Consultar las asignaciones propias'),
    -- Financiero
    ('FINANZAS',     'FINANZAS_VER',             'Consultar ingresos, gastos, costos y márgenes'),
    ('FINANZAS',     'FINANZAS_GESTIONAR',       'Registrar y anular movimientos financieros'),
    -- Informacion gerencial
    ('GERENCIAL',    'REPORTES_VER',             'Consultar reportes, KPIs y alertas'),
    -- Subcontratacion y consumibles
    ('PROVEEDORES',  'PROVEEDORES_VER',          'Consultar proveedores, servicios externos y consumibles'),
    ('PROVEEDORES',  'PROVEEDORES_GESTIONAR',    'Registrar proveedores, servicios externos y consumibles'),
    ('PROVEEDORES',  'CONSUMIBLES_REGISTRAR',    'Registrar el consumo de consumibles en proyectos');

-- Administrador: todos los permisos
INSERT INTO roles_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r CROSS JOIN permisos p
WHERE r.codigo = 'ADMINISTRADOR';

-- Gerente: consulta general, aprobacion, finanzas, reportes y auditoria
INSERT INTO roles_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r JOIN permisos p ON p.nombre IN (
    'USUARIOS_VER', 'ROLES_VER', 'AUDITORIA_VER',
    'ACTIVOS_VER', 'COMERCIAL_VER', 'COTIZACIONES_APROBAR',
    'LOGISTICA_VER', 'FINANZAS_VER', 'FINANZAS_GESTIONAR',
    'REPORTES_VER', 'PROVEEDORES_VER')
WHERE r.codigo = 'GERENTE';

-- Tecnico: activos, mantenimientos, asignaciones propias y consumos
INSERT INTO roles_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r JOIN permisos p ON p.nombre IN (
    'ACTIVOS_VER', 'MANTENIMIENTOS_REGISTRAR',
    'ASIGNACIONES_VER_PROPIAS', 'CONSUMIBLES_REGISTRAR')
WHERE r.codigo = 'TECNICO';

-- Sub-contratado temporal: solo sus asignaciones
INSERT INTO roles_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r JOIN permisos p ON p.nombre IN ('ASIGNACIONES_VER_PROPIAS')
WHERE r.codigo = 'SUBCONTRATADO_TEMPORAL';
