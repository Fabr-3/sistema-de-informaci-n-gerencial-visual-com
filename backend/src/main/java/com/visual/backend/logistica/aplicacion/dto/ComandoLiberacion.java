package com.visual.backend.logistica.aplicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComandoLiberacion(@NotBlank @Size(max = 255) String motivo) {
}
