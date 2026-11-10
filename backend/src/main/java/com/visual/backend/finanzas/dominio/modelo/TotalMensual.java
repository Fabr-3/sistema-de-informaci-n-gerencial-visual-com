package com.visual.backend.finanzas.dominio.modelo;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Ingresos y egresos registrados en un mes. */
public record TotalMensual(YearMonth mes, BigDecimal ingresos, BigDecimal egresos) {

    public static TotalMensual vacio(YearMonth mes) {
        return new TotalMensual(mes, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
