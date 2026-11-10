package com.visual.backend.finanzas.dominio.modelo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tablero del gerente: el periodo frente al anterior de igual duracion, la
 * evolucion de los ultimos meses, el margen de los proyectos, las cuentas por
 * cobrar y los movimientos recientes de monto significativo.
 */
public record IndicadoresFinancieros(ResumenPeriodo actual, ResumenPeriodo anterior, List<TotalMensual> evolucion,
                                     List<ResultadoProyecto> margenProyectos,
                                     List<ResultadoProyecto> cuentasPorCobrar, BigDecimal totalPorCobrar,
                                     List<MovimientoDestacado> movimientosSignificativos,
                                     BigDecimal umbralSignificativo) {
}
