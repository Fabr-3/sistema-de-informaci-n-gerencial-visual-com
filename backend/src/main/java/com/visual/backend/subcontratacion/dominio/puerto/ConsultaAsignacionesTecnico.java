package com.visual.backend.subcontratacion.dominio.puerto;

import java.time.LocalDateTime;
import java.util.List;

import com.visual.backend.subcontratacion.dominio.modelo.EventoAsignado;

/** Eventos donde trabaja el usuario actual, segun la planificacion logistica. */
public interface ConsultaAsignacionesTecnico {

    List<EventoAsignado> eventosAsignados(LocalDateTime desde);
}
