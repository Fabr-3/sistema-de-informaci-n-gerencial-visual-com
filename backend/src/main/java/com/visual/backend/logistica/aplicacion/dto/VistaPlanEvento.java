package com.visual.backend.logistica.aplicacion.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.visual.backend.logistica.dominio.modelo.EventoLogistico;

/**
 * Planificacion logistica de un evento. La ventana va del primer montaje al
 * ultimo desmontaje; los avisos senalan jornadas desfasadas del evento.
 */
public record VistaPlanEvento(EventoLogistico evento, boolean editable, String detallesTransporte,
                              LocalDateTime ventanaInicio, LocalDateTime ventanaFin, List<VistaJornada> jornadas,
                              List<VistaAsignacion> asignaciones, List<String> avisos) {
}
