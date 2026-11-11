package com.visual.backend.gerencial.dominio.modelo;

import java.math.BigDecimal;

/**
 * Ingresos y costos imputados a los proyectos de un cliente en un periodo.
 * proyectosContratados cuenta todos sus proyectos no cancelados hasta el fin
 * del periodo; proyectosConMovimientos, los que tuvieron movimientos en el.
 */
public record ResultadoCliente(Integer idCliente, String cliente, int proyectosContratados,
                               int proyectosConMovimientos, BigDecimal ingresos, BigDecimal costos,
                               BigDecimal margen, BigDecimal porcentaje) {

    public static ResultadoCliente de(Integer idCliente, String cliente, int proyectosContratados,
                                      int proyectosConMovimientos, BigDecimal ingresos, BigDecimal costos) {
        BigDecimal margen = ingresos.subtract(costos);
        return new ResultadoCliente(idCliente, cliente, proyectosContratados, proyectosConMovimientos, ingresos,
                costos, margen, Porcentaje.de(margen, ingresos));
    }

    public boolean esRecurrente(int minimoProyectos) {
        return proyectosContratados >= minimoProyectos;
    }
}
