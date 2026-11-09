package com.visual.backend.activos.aplicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComandoModuloLed(@NotBlank @Size(max = 50) String codigo) {
}
