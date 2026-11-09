package com.visual.backend.accesos.dominio.modelo;

/**
 * Codigos de los permisos fijos (permisos.nombre). Se usan como autoridades
 * de seguridad, por lo que deben coincidir con los datos iniciales (V3).
 */
public final class Permisos {

    public static final String USUARIOS_VER = "USUARIOS_VER";
    public static final String USUARIOS_GESTIONAR = "USUARIOS_GESTIONAR";
    public static final String ROLES_VER = "ROLES_VER";
    public static final String ROLES_GESTIONAR = "ROLES_GESTIONAR";
    public static final String AUDITORIA_VER = "AUDITORIA_VER";
    public static final String RESPALDOS_GESTIONAR = "RESPALDOS_GESTIONAR";
    public static final String ACTIVOS_VER = "ACTIVOS_VER";
    public static final String ACTIVOS_GESTIONAR = "ACTIVOS_GESTIONAR";
    public static final String MANTENIMIENTOS_REGISTRAR = "MANTENIMIENTOS_REGISTRAR";
    public static final String COMERCIAL_VER = "COMERCIAL_VER";
    public static final String COMERCIAL_GESTIONAR = "COMERCIAL_GESTIONAR";
    public static final String COTIZACIONES_APROBAR = "COTIZACIONES_APROBAR";
    public static final String LOGISTICA_VER = "LOGISTICA_VER";
    public static final String LOGISTICA_GESTIONAR = "LOGISTICA_GESTIONAR";
    public static final String ASIGNACIONES_VER_PROPIAS = "ASIGNACIONES_VER_PROPIAS";

    private Permisos() {
    }
}
