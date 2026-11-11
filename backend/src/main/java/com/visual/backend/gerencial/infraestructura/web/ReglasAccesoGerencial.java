package com.visual.backend.gerencial.infraestructura.web;

import com.visual.backend.accesos.dominio.modelo.Permisos;

/** Expresiones de autorizacion del modulo de informacion gerencial. */
final class ReglasAccesoGerencial {

    static final String VER = "hasAuthority('" + Permisos.REPORTES_VER + "')";

    private ReglasAccesoGerencial() {
    }
}
