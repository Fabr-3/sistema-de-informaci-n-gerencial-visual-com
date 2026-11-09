package com.visual.backend.logistica.dominio.modelo;

/** Evento con el avance de su planificacion: jornadas, personas asignadas y cruces. */
public record ResumenLogisticoEvento(EventoLogistico evento, long jornadas, long personal, long cruces) {
}
