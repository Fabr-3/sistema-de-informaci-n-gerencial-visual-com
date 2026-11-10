package com.visual.backend.finanzas.dominio.puerto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.visual.backend.finanzas.dominio.modelo.MovimientoDestacado;
import com.visual.backend.finanzas.dominio.modelo.SumasPeriodo;
import com.visual.backend.finanzas.dominio.modelo.TotalMensual;

/** Agregados de movimientos registrados (los anulados nunca se cuentan); fechas incluidas. */
public interface ConsultaIndicadores {

    SumasPeriodo sumas(LocalDate desde, LocalDate hasta);

    /** Solo los meses con movimientos; el servicio completa los meses vacios. */
    List<TotalMensual> totalesMensuales(LocalDate desde, LocalDate hasta);

    /** Movimientos con monto mayor o igual al umbral, del mayor al menor. */
    List<MovimientoDestacado> destacados(LocalDate desde, LocalDate hasta, BigDecimal umbral, int limite);
}
