package com.visual.backend.activos.aplicacion.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Mismo periodo de uso para varios equipos (por ejemplo, todos los de un evento). */
public record ComandoUsoGrupal(@NotEmpty @Size(max = 100) List<@NotNull Integer> idsActivos,
                               @NotNull LocalDateTime fechaInicio, @NotNull LocalDateTime fechaFin,
                               @Size(max = 255) String descripcion) {
}
