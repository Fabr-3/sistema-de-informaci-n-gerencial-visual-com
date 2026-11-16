package com.visual.backend.subcontratacion.aplicacion.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** costoUnitario, si se indica, pasa a ser el costo vigente del consumible. */
public record ComandoIngresoConsumible(@NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal cantidad,
                                       @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal costoUnitario,
                                       @Size(max = 255) String motivo) {
}
