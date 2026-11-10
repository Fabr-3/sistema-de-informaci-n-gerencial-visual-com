package com.visual.backend.finanzas.dominio.modelo;

import java.math.BigDecimal;

/** Sumas de los movimientos registrados de un periodo, separadas por su imputacion. */
public record SumasPeriodo(BigDecimal ingresos, BigDecimal costosDirectos, BigDecimal gastosGenerales) {
}
