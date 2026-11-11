package com.visual.backend.gerencial.dominio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Porcentajes de los indicadores con un decimal; nulos cuando la base es cero. */
final class Porcentaje {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final int DECIMALES = 1;
    private static final int DECIMALES_DIVISION = 6;

    private Porcentaje() {
    }

    static BigDecimal de(BigDecimal parte, BigDecimal total) {
        if (total.signum() == 0) {
            return null;
        }
        return parte.divide(total, DECIMALES_DIVISION, RoundingMode.HALF_UP).multiply(CIEN)
                .setScale(DECIMALES, RoundingMode.HALF_UP);
    }

    static BigDecimal de(long parte, long total) {
        return de(BigDecimal.valueOf(parte), BigDecimal.valueOf(total));
    }
}
