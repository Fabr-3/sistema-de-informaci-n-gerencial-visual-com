package com.visual.backend.activos.aplicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComandoReemplazoModulo(@NotBlank @Size(max = 50) String codigoNuevo,
                                     @NotBlank @Size(max = 500) String motivo) {
}
