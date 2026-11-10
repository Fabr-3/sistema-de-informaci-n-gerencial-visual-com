package com.visual.backend.finanzas.aplicacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Registro de un movimiento; los items solo se envian en las ventas de equipos. */
public record ComandoMovimiento(@NotNull Integer idTipo,
                                @NotBlank @Size(max = 200) String concepto,
                                @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal monto,
                                @NotNull LocalDate fecha,
                                Integer idProyecto,
                                Integer idEvento,
                                Integer idProveedor,
                                @Size(max = 30) String numeroFactura,
                                @Size(max = 100) List<@Valid Item> items) {

    /** Una unidad (idActivo) o una cantidad de una existencia por lote (idExistencia). */
    public record Item(Integer idActivo, Integer idExistencia, @Positive Integer cantidad) {

        @AssertTrue(message = "indique una unidad o un lote, no ambos")
        public boolean isUnicoOrigen() {
            return (idActivo == null) != (idExistencia == null);
        }
    }
}
