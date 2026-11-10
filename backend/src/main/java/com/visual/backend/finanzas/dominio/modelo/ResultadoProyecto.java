package com.visual.backend.finanzas.dominio.modelo;

import java.math.BigDecimal;

/**
 * Resultado economico de un proyecto. Lo por cobrar es el importe contratado
 * (cotizacion aceptada) menos lo ya cobrado; nunca es negativo.
 */
public record ResultadoProyecto(Integer idProyecto, String proyecto, String cliente, String estado,
                                BigDecimal contratado, Margen margen, BigDecimal porCobrar) {

    public static ResultadoProyecto de(Integer idProyecto, String proyecto, String cliente, String estado,
                                       BigDecimal contratado, BigDecimal ingresos, BigDecimal costos) {
        return new ResultadoProyecto(idProyecto, proyecto, cliente, estado, contratado,
                Margen.calcular(ingresos, costos), contratado.subtract(ingresos).max(BigDecimal.ZERO));
    }
}
