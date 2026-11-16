package com.visual.backend.activos.aplicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ComandoReasignacionModulo(@NotNull Integer idActivoDestino, @NotBlank @Size(max = 500) String motivo) {
}
