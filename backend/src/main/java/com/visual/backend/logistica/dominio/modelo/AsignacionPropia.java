package com.visual.backend.logistica.dominio.modelo;

import java.time.LocalDateTime;

/** Asignacion vista por la persona asignada (incluye lugar y transporte). */
public record AsignacionPropia(Integer id, Integer idPlan, Integer idPersonal, EventoLogistico evento, String rol, LocalDateTime inicio,
                               LocalDateTime fin, String detallesTransporte) {
}
