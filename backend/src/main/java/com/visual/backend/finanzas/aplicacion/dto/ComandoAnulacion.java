package com.visual.backend.finanzas.aplicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComandoAnulacion(@NotBlank @Size(max = 1000) String motivo) {
}
