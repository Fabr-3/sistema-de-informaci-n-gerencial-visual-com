package com.visual.backend.subcontratacion.infraestructura.web;

import com.visual.backend.accesos.dominio.modelo.Permisos;

/** Expresiones de autorizacion del modulo de subcontratacion y consumibles. */
final class ReglasAccesoSubcontratacion {

    static final String VER = "hasAnyAuthority('" + Permisos.PROVEEDORES_VER + "', '"
            + Permisos.PROVEEDORES_GESTIONAR + "')";
    static final String GESTIONAR = "hasAuthority('" + Permisos.PROVEEDORES_GESTIONAR + "')";
    /** Tecnicos (en sus eventos) y administrador. */
    static final String REGISTRAR_CONSUMOS = "hasAuthority('" + Permisos.CONSUMIBLES_REGISTRAR + "')";
    /** El catalogo y los consumos tambien los consulta quien registra consumos. */
    static final String VER_CONSUMOS = "hasAnyAuthority('" + Permisos.PROVEEDORES_VER + "', '"
            + Permisos.PROVEEDORES_GESTIONAR + "', '" + Permisos.CONSUMIBLES_REGISTRAR + "')";

    private ReglasAccesoSubcontratacion() {
    }
}
