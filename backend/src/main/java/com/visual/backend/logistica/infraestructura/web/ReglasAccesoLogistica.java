package com.visual.backend.logistica.infraestructura.web;

import com.visual.backend.accesos.dominio.modelo.Permisos;

/** Expresiones de autorizacion del modulo de logistica. */
final class ReglasAccesoLogistica {

    static final String VER = "hasAuthority('" + Permisos.LOGISTICA_VER + "')";
    static final String GESTIONAR = "hasAuthority('" + Permisos.LOGISTICA_GESTIONAR + "')";
    /** Tecnicos y sub-contratados consultan solo sus asignaciones. */
    static final String PROPIAS = "hasAuthority('" + Permisos.ASIGNACIONES_VER_PROPIAS + "')";

    private ReglasAccesoLogistica() {
    }
}
