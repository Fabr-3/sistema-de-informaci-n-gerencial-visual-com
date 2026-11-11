package com.visual.backend.gerencial.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * KPIs financieros de un periodo. La utilidad bruta descuenta los costos
 * directos (egresos imputados a proyectos); la ganancia (utilidad neta)
 * descuenta ademas los gastos generales. Los margenes se expresan sobre los
 * ingresos y son nulos cuando no hubo ingresos.
 */
public record IndicadoresPeriodo(LocalDate desde, LocalDate hasta, BigDecimal ingresos, BigDecimal costosDirectos,
                                 BigDecimal gastosGenerales, BigDecimal utilidadBruta, BigDecimal ganancia,
                                 BigDecimal margenBruto, BigDecimal margenNeto) {

    public static IndicadoresPeriodo calcular(Periodo periodo, BigDecimal ingresos, BigDecimal costosDirectos,
                                              BigDecimal gastosGenerales) {
        BigDecimal utilidadBruta = ingresos.subtract(costosDirectos);
        BigDecimal ganancia = utilidadBruta.subtract(gastosGenerales);
        return new IndicadoresPeriodo(periodo.desde(), periodo.hasta(), ingresos, costosDirectos, gastosGenerales,
                utilidadBruta, ganancia, Porcentaje.de(utilidadBruta, ingresos), Porcentaje.de(ganancia, ingresos));
    }
}
