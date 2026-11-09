package com.visual.backend.activos.aplicacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Edicion de un activo. El codigo de inventario no se modifica. */
public record ComandoActualizarActivo(@Size(max = 100) String numeroSerie,
                                      @NotNull Integer idModelo,
                                      @NotNull Integer idBodega,
                                      @PastOrPresent LocalDate fechaAdquisicion,
                                      @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal costoAdquisicion,
                                      @Size(max = 2000) String observaciones) {
}
