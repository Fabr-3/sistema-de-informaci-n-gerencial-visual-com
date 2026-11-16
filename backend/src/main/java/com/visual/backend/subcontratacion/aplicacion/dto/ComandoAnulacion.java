package com.visual.backend.subcontratacion.aplicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComandoAnulacion(@NotBlank @Size(max = 500) String motivo) {
}
