package com.visual.backend.activos.aplicacion.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ComandoIniciarMantenimiento(@NotNull Integer idTipoMantenimiento, @NotNull LocalDateTime fechaInicio,
                                          @Size(max = 2000) String descripcion) {
}
