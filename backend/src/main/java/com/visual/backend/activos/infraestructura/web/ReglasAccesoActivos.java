package com.visual.backend.activos.infraestructura.web;

import com.visual.backend.accesos.dominio.modelo.Permisos;

/** Expresiones de autorizacion del modulo de activos para @PreAuthorize. */
final class ReglasAccesoActivos {

    static final String VER = "hasAuthority('" + Permisos.ACTIVOS_VER + "')";
    static final String GESTIONAR = "hasAuthority('" + Permisos.ACTIVOS_GESTIONAR + "')";
    static final String REGISTRAR_MANTENIMIENTO = "hasAuthority('" + Permisos.MANTENIMIENTOS_REGISTRAR + "')";
    /** Corregir el historial o el estado de modulos: tecnicos o administradores del inventario. */
    static final String GESTIONAR_O_REGISTRAR = "hasAnyAuthority('" + Permisos.ACTIVOS_GESTIONAR + "', '"
            + Permisos.MANTENIMIENTOS_REGISTRAR + "')";

    private ReglasAccesoActivos() {
    }
}
