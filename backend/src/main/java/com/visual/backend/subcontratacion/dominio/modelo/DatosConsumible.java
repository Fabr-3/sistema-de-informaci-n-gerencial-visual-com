package com.visual.backend.subcontratacion.dominio.modelo;

import java.math.BigDecimal;

/** stockMinimo es opcional: sin el no se advierte stock bajo. */
public record DatosConsumible(String nombre, String unidadMedida, BigDecimal stockMinimo, BigDecimal costoUnitario,
                              boolean activo) {
}
