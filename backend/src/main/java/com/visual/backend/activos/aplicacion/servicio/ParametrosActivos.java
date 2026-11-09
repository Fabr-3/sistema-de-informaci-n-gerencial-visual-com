package com.visual.backend.activos.aplicacion.servicio;

/**
 * Parametros configurables del modulo (los define la infraestructura).
 * porcentajeAlerta: porcentaje del intervalo de horas a partir del cual se
 * avisa que el mantenimiento preventivo esta proximo.
 */
public record ParametrosActivos(int porcentajeAlerta) {
}
