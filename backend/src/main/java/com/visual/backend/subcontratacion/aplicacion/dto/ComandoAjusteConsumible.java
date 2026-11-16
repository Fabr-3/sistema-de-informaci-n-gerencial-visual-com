package com.visual.backend.subcontratacion.aplicacion.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** cantidad lleva signo: positiva suma al stock y negativa lo reduce. */
public record ComandoAjusteConsumible(@NotNull @Digits(integer = 10, fraction = 2) BigDecimal cantidad,
                                      @NotBlank @Size(max = 255) String motivo) {
}
