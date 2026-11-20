package com.visual.backend.subcontratacion.aplicacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Datos del gasto; el comprobante llega como archivo aparte en la misma solicitud. */
public record ComandoGasto(@NotNull Integer idProyecto, @NotNull Integer idEvento,
                           @NotBlank @Size(max = 500) String descripcion,
                           @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal monto,
                           @NotNull LocalDate fecha) {
}
