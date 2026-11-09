package com.visual.backend.logistica.aplicacion.dto;

import java.util.List;

import com.visual.backend.logistica.dominio.modelo.Cruce;

/** Personal activo con las asignaciones que ya tiene en el horario consultado. */
public record PersonalDisponible(Integer id, String nombreCompleto, List<String> especialidades,
                                 List<Cruce> cruces) {
}
