package com.visual.backend.activos.aplicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ComandoBodega(@NotBlank @Size(max = 100) String nombre, @Size(max = 500) String direccion,
                            @NotNull Boolean activa) {
}
