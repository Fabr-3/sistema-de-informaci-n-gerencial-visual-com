package com.visual.backend.comercial.aplicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComandoLugar(@NotBlank @Size(max = 150) String nombre, @NotBlank @Size(max = 500) String direccion,
                           @Size(max = 80) String ciudad) {
}
