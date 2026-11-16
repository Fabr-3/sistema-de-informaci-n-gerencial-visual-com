package com.visual.backend.subcontratacion.dominio.modelo;

import java.time.LocalDateTime;

/** Evento donde trabaja el tecnico que registra consumos. */
public record EventoAsignado(Integer idEvento, String evento, Integer idProyecto, String proyecto,
                             LocalDateTime fechaInicio) {
}
