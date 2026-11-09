package com.visual.backend.activos.aplicacion.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ComandoRegistroUso(@NotNull LocalDateTime fechaInicio, @NotNull LocalDateTime fechaFin,
                                 @Size(max = 255) String descripcion) {
}
