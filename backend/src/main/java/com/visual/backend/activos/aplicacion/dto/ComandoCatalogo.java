package com.visual.backend.activos.aplicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Alta o edicion de una categoria o un fabricante. */
public record ComandoCatalogo(@NotBlank @Size(max = 100) String nombre) {
}
