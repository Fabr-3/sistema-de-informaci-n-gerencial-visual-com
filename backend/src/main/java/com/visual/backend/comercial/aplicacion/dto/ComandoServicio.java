package com.visual.backend.comercial.aplicacion.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ComandoServicio(@NotBlank @Size(max = 100) String nombre,
                              @Size(max = 1000) String descripcion,
                              @NotBlank @Size(max = 30) String unidadMedida,
                              @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal precioReferencial,
                              @NotNull Boolean activo) {
}
