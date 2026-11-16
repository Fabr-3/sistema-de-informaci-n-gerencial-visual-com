package com.visual.backend.subcontratacion.aplicacion.dto;

import java.math.BigDecimal;

import com.visual.backend.subcontratacion.dominio.modelo.DatosConsumible;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ComandoConsumible(@NotBlank @Size(max = 120) String nombre, @NotBlank @Size(max = 30) String unidadMedida,
                                @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal stockMinimo,
                                @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal costoUnitario,
                                boolean activo) {

    public DatosConsumible datos() {
        return new DatosConsumible(nombre, unidadMedida, stockMinimo, costoUnitario, activo);
    }
}
