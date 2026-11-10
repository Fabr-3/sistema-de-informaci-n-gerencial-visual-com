package com.visual.backend.finanzas.dominio.modelo;

import java.util.List;

/** Resultado del proyecto desglosado por evento y por tipo de movimiento. */
public record DetalleResultadoProyecto(ResultadoProyecto proyecto, List<ResultadoEvento> eventos,
                                       List<TotalPorTipo> porTipo) {
}
