package com.visual.backend.finanzas.infraestructura.web;

import com.visual.backend.accesos.dominio.modelo.Permisos;

/** Expresiones de autorizacion del modulo financiero. */
final class ReglasAccesoFinanzas {

    static final String VER = "hasAuthority('" + Permisos.FINANZAS_VER + "')";
    static final String GESTIONAR = "hasAuthority('" + Permisos.FINANZAS_GESTIONAR + "')";
    /** Los proveedores tambien los consulta y registra el modulo de proveedores. */
    static final String VER_PROVEEDORES = "hasAnyAuthority('" + Permisos.FINANZAS_VER + "', '"
            + Permisos.PROVEEDORES_VER + "')";
    static final String GESTIONAR_PROVEEDORES = "hasAnyAuthority('" + Permisos.FINANZAS_GESTIONAR + "', '"
            + Permisos.PROVEEDORES_GESTIONAR + "')";

    private ReglasAccesoFinanzas() {
    }
}
