package com.visual.backend.activos.aplicacion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.visual.backend.activos.dominio.modelo.CondicionFinal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ComandoFinalizarMantenimiento(@NotNull LocalDateTime fechaFin, @NotNull CondicionFinal condicionFinal,
                                            @Size(max = 2000) String observaciones,
                                            @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal costo) {
}
