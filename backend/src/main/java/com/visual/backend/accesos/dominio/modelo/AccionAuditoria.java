package com.visual.backend.accesos.dominio.modelo;

/** Acciones auditables. Deben coincidir con el CHECK de auditoria.accion. */
public enum AccionAuditoria {
    INICIO_SESION,
    INICIO_SESION_FALLIDO,
    CIERRE_SESION,
    BLOQUEO_CUENTA,
    CAMBIO_CONTRASENA,
    CREAR,
    ACTUALIZAR,
    ELIMINAR,
    RESPALDO_CREADO,
    RESPALDO_DESCARGADO
}
