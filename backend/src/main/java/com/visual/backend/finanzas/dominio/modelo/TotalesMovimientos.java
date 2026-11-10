package com.visual.backend.finanzas.dominio.modelo;

import java.math.BigDecimal;

/** Sumas de los movimientos registrados (no anulados) que cumplen un filtro. */
public record TotalesMovimientos(BigDecimal ingresos, BigDecimal egresos) {

    public BigDecimal saldo() {
        return ingresos.subtract(egresos);
    }
}
