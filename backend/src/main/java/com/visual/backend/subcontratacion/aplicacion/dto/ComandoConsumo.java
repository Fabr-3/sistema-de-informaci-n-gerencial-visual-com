package com.visual.backend.subcontratacion.aplicacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** idEvento es opcional para quien gestiona; el tecnico siempre indica un evento donde esta asignado. */
public record ComandoConsumo(@NotNull Integer idConsumible, @NotNull Integer idProyecto, Integer idEvento,
                             @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal cantidad,
                             @NotNull LocalDate fecha) {
}
