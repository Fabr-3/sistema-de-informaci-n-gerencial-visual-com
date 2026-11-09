package com.visual.backend.comercial.infraestructura.web;

import com.visual.backend.accesos.dominio.modelo.Permisos;

/** Expresiones de autorizacion del modulo comercial para @PreAuthorize. */
final class ReglasAccesoComercial {

    static final String VER = "hasAuthority('" + Permisos.COMERCIAL_VER + "')";
    static final String GESTIONAR = "hasAuthority('" + Permisos.COMERCIAL_GESTIONAR + "')";
    /** Aceptar o rechazar cotizaciones (gerencia). */
    static final String APROBAR = "hasAuthority('" + Permisos.COTIZACIONES_APROBAR + "')";

    private ReglasAccesoComercial() {
    }
}
