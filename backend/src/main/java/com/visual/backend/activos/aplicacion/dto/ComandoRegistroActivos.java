package com.visual.backend.activos.aplicacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Registro de una o varias unidades del mismo modelo. Los codigos se generan
 * con el prefijo del modelo; solo al registrar una unidad se admite un codigo
 * manual. Los numeros de serie son opcionales: si se envian, uno por unidad.
 * El costo es por unidad.
 */
public record ComandoRegistroActivos(@NotNull Integer idModelo,
                                     @NotNull Integer idBodega,
                                     @NotNull @Min(1) @Max(ComandoRegistroActivos.CANTIDAD_MAXIMA) Integer cantidad,
                                     @Size(max = 50) String codigoInventario,
                                     @Size(max = CANTIDAD_MAXIMA) List<@Size(max = 100) String> numerosSerie,
                                     @PastOrPresent LocalDate fechaAdquisicion,
                                     @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal costoUnitario,
                                     @Size(max = 2000) String observaciones) {

    /** Limite por operacion para evitar registros masivos accidentales. */
    public static final int CANTIDAD_MAXIMA = 100;
}
