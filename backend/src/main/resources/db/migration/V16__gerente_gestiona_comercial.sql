-- =====================================================================
-- V16 - El gerente realiza todo el flujo comercial: registra clientes,
-- cotizaciones, proyectos, contratos y eventos (ademas de aprobar las
-- cotizaciones y planificar la logistica). El administrador lo conserva.
-- =====================================================================

INSERT INTO roles_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r JOIN permisos p ON p.nombre = 'COMERCIAL_GESTIONAR'
WHERE r.codigo = 'GERENTE'
ON CONFLICT DO NOTHING;
