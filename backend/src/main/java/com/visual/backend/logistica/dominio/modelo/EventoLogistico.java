package com.visual.backend.logistica.dominio.modelo;

import java.time.LocalDateTime;

import com.visual.backend.comercial.dominio.modelo.EstadoEvento;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/** Datos del evento comercial que necesita la planificacion logistica. */
public record EventoLogistico(Integer id, String nombre, LocalDateTime fechaInicio, LocalDateTime fechaFin,
                              EstadoEvento estado, Integer idProyecto, String proyecto, String cliente,
                              String lugar, String direccion, String ciudad) {

    public Periodo periodo() {
        return new Periodo(fechaInicio, fechaFin);
    }

    /** Solo la logistica de un evento programado se puede modificar. */
    public boolean editable() {
        return estado == EstadoEvento.PROGRAMADO;
    }

    public void exigirEditable() {
        if (!editable()) {
            throw new ReglaNegocioExcepcion("La logística de un evento " + estado.name().toLowerCase()
                    + " ya no se puede modificar");
        }
    }
}
