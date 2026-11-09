package com.visual.backend.logistica.aplicacion.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.visual.backend.logistica.dominio.modelo.Cruce;
import com.visual.backend.logistica.dominio.modelo.EventoLogistico;

public record VistaAsignacionPropia(Integer id, EventoLogistico evento, String rol, LocalDateTime inicio,
                                    LocalDateTime fin, String detallesTransporte, List<Cruce> cruces) {
}
