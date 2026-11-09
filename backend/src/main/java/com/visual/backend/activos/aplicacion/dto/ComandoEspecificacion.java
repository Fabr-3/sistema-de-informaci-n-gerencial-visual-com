package com.visual.backend.activos.aplicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComandoEspecificacion(@NotBlank @Size(max = 100) String caracteristica,
                                    @NotBlank @Size(max = 150) String valor) {
}
