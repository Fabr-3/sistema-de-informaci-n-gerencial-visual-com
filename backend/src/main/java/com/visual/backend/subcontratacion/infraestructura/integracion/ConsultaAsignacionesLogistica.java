package com.visual.backend.subcontratacion.infraestructura.integracion;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.visual.backend.logistica.aplicacion.servicio.ServicioAsignacionesPropias;
import com.visual.backend.subcontratacion.dominio.modelo.EventoAsignado;
import com.visual.backend.subcontratacion.dominio.puerto.ConsultaAsignacionesTecnico;

/** Eventos del tecnico segun las asignaciones propias del modulo de logistica. */
@Component
public class ConsultaAsignacionesLogistica implements ConsultaAsignacionesTecnico {

    private final ServicioAsignacionesPropias asignaciones;

    public ConsultaAsignacionesLogistica(ServicioAsignacionesPropias asignaciones) {
        this.asignaciones = asignaciones;
    }

    @Override
    public List<EventoAsignado> eventosAsignados(LocalDateTime desde) {
        return asignaciones.eventosAsignados(desde).stream()
                .map(evento -> new EventoAsignado(evento.id(), evento.nombre(), evento.idProyecto(), evento.proyecto(),
                        evento.fechaInicio()))
                .toList();
    }
}
