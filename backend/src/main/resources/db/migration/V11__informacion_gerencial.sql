-- =====================================================================
-- V11 - Modulo de informacion gerencial.
--   * La emision de cada reporte operativo queda registrada en la
--     auditoria (los reportes contienen informacion financiera).
--   * Los KPIs y las alertas se calculan sobre los datos existentes; no
--     requieren tablas nuevas. El permiso REPORTES_VER existe desde V3.
-- =====================================================================

ALTER TABLE auditoria DROP CONSTRAINT auditoria_accion_check;
ALTER TABLE auditoria ADD CONSTRAINT auditoria_accion_check
    CHECK (accion IN ('INICIO_SESION', 'INICIO_SESION_FALLIDO', 'CIERRE_SESION',
                      'BLOQUEO_CUENTA', 'CAMBIO_CONTRASENA',
                      'CREAR', 'ACTUALIZAR', 'ELIMINAR',
                      'RESPALDO_CREADO', 'RESPALDO_DESCARGADO', 'REPORTE_GENERADO'));
