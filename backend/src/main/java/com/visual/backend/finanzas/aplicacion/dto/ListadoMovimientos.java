package com.visual.backend.finanzas.aplicacion.dto;

import java.math.BigDecimal;

import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.finanzas.dominio.modelo.MovimientoFinanciero;
import com.visual.backend.finanzas.dominio.modelo.TotalesMovimientos;

/** Pagina de movimientos con los totales (no anulados) de todo el filtro. */
public record ListadoMovimientos(Pagina<MovimientoFinanciero> pagina, BigDecimal ingresos, BigDecimal egresos,
                                 BigDecimal saldo) {

    public static ListadoMovimientos de(Pagina<MovimientoFinanciero> pagina, TotalesMovimientos totales) {
        return new ListadoMovimientos(pagina, totales.ingresos(), totales.egresos(), totales.saldo());
    }
}
