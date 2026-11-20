package com.visual.backend.subcontratacion.aplicacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * idServicio e idEvento son opcionales. idModelo y cantidad registran un
 * subalquiler de equipos (exigen el evento).
 */
public record ComandoSubcontratacion(@NotNull Integer idProveedor, Integer idServicio, @NotNull Integer idProyecto,
                                     Integer idEvento, @NotBlank @Size(max = 2000) String descripcion,
                                     @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal costo,
                                     @NotNull LocalDate fecha, Integer idModelo,
                                     @Positive @Max(500) Integer cantidad) {
}
