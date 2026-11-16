package com.visual.backend.subcontratacion.dominio.modelo;

import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/** Proyecto del modulo comercial; vigente si esta en planificacion o en ejecucion. */
public record ProyectoReferencia(Integer id, String nombre, boolean vigente) {

    public ElementoCatalogo exigirVigente() {
        if (!vigente) {
            throw new ReglaNegocioExcepcion("El proyecto " + nombre + " no está en planificación ni en ejecución");
        }
        return new ElementoCatalogo(id, nombre);
    }
}
