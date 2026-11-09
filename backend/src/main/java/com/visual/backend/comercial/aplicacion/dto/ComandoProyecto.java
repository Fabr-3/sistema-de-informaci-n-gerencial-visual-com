package com.visual.backend.comercial.aplicacion.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComandoProyecto(@NotBlank @Size(max = 200) String nombre, @Size(max = 2000) String descripcion,
                              LocalDate fechaInicio, LocalDate fechaFin) {
}
