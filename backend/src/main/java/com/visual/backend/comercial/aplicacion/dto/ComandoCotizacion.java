package com.visual.backend.comercial.aplicacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ComandoCotizacion(@NotNull Integer idCliente,
                                @NotBlank @Size(max = 200) String titulo,
                                @NotNull LocalDate fechaEmision,
                                @NotNull LocalDate fechaValidez,
                                LocalDate fechaEventoInicio,
                                LocalDate fechaEventoFin,
                                @Size(max = 4000) String condiciones,
                                @NotEmpty @Size(max = 50) List<@Valid Linea> lineas) {

    public record Linea(@NotNull Integer idServicio,
                        @NotNull @Min(1) @Max(100_000) Integer cantidad,
                        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal precioUnitario,
                        @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal descuento) {
    }
}
