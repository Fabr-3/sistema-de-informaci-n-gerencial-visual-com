package com.visual.backend.finanzas.dominio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Resultado de un proyecto, evento o periodo: margen = ingresos - costos
 * directos (sin sueldos). El porcentaje se calcula sobre los ingresos y es
 * nulo cuando aun no hay ingresos.
 */
public record Margen(BigDecimal ingresos, BigDecimal costos, BigDecimal margen, BigDecimal porcentaje) {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final int DECIMALES_PORCENTAJE = 1;
    private static final int DECIMALES_DIVISION = 6;

    public static Margen calcular(BigDecimal ingresos, BigDecimal costos) {
        BigDecimal margen = ingresos.subtract(costos);
        BigDecimal porcentaje = ingresos.signum() == 0 ? null
                : margen.divide(ingresos, DECIMALES_DIVISION, RoundingMode.HALF_UP).multiply(CIEN)
                        .setScale(DECIMALES_PORCENTAJE, RoundingMode.HALF_UP);
        return new Margen(ingresos, costos, margen, porcentaje);
    }
}
