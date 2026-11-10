package com.visual.backend.finanzas.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Movimiento reciente de monto alto que el gerente debe conocer. */
public record MovimientoDestacado(Integer id, LocalDate fecha, String tipo, Naturaleza naturaleza, String concepto,
                                  BigDecimal monto, String proyecto) {
}
