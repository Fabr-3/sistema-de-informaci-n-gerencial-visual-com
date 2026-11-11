package com.visual.backend.gerencial.dominio.modelo;

/**
 * Tablero de informacion gerencial: KPIs financieros del periodo frente al
 * anterior de igual duracion, rentabilidad recurrente anual, utilizacion de
 * recursos del periodo y resumen de las alertas vigentes de activos.
 */
public record KpisGerenciales(IndicadoresPeriodo actual, IndicadoresPeriodo anterior,
                              RentabilidadRecurrente rentabilidadRecurrente, UtilizacionRecursos utilizacion,
                              ResumenAlertas alertas) {
}
