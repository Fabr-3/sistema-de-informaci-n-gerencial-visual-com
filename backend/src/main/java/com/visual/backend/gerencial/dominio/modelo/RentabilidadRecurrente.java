package com.visual.backend.gerencial.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Rentabilidad recurrente anual basada en proyectos: en los ultimos doce
 * meses, el margen (ingresos - costos directos) de los proyectos de clientes
 * recurrentes (con un minimo de proyectos contratados) sobre sus ingresos.
 * participacion es la parte del margen anual de todos los proyectos que
 * aportan esos clientes; es nula si el margen anual no es positivo.
 */
public record RentabilidadRecurrente(LocalDate desde, LocalDate hasta, int minimoProyectos,
                                     int clientesRecurrentes, int clientesConProyectos, BigDecimal ingresos,
                                     BigDecimal costos, BigDecimal margen, BigDecimal porcentaje,
                                     BigDecimal margenTotalProyectos, BigDecimal participacion,
                                     List<ResultadoCliente> clientes) {

    public static RentabilidadRecurrente calcular(Periodo anual, List<ResultadoCliente> resultados,
                                                  int minimoProyectos) {
        List<ResultadoCliente> recurrentes = resultados.stream()
                .filter(cliente -> cliente.esRecurrente(minimoProyectos))
                .sorted(Comparator.comparing(ResultadoCliente::margen).reversed())
                .toList();
        BigDecimal ingresos = sumar(recurrentes, ResultadoCliente::ingresos);
        BigDecimal costos = sumar(recurrentes, ResultadoCliente::costos);
        BigDecimal margen = ingresos.subtract(costos);
        BigDecimal margenTotal = sumar(resultados, ResultadoCliente::margen);
        return new RentabilidadRecurrente(anual.desde(), anual.hasta(), minimoProyectos, recurrentes.size(),
                resultados.size(), ingresos, costos, margen, Porcentaje.de(margen, ingresos), margenTotal,
                margenTotal.signum() > 0 ? Porcentaje.de(margen, margenTotal) : null, recurrentes);
    }

    private static BigDecimal sumar(List<ResultadoCliente> clientes, Function<ResultadoCliente, BigDecimal> valor) {
        return clientes.stream().map(valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
