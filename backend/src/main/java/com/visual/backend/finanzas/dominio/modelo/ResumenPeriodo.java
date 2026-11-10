package com.visual.backend.finanzas.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resultado de un periodo. Los costos directos son los egresos imputados a
 * proyectos; los gastos generales, los egresos sin proyecto. El margen bruto
 * descuenta solo los costos directos y el resultado neto, todos los egresos.
 */
public record ResumenPeriodo(LocalDate desde, LocalDate hasta, BigDecimal ingresos, BigDecimal costosDirectos,
                             BigDecimal gastosGenerales, Margen margenBruto, BigDecimal resultadoNeto) {

    public static ResumenPeriodo de(LocalDate desde, LocalDate hasta, BigDecimal ingresos, BigDecimal costosDirectos,
                                    BigDecimal gastosGenerales) {
        return new ResumenPeriodo(desde, hasta, ingresos, costosDirectos, gastosGenerales,
                Margen.calcular(ingresos, costosDirectos), ingresos.subtract(costosDirectos).subtract(gastosGenerales));
    }
}
