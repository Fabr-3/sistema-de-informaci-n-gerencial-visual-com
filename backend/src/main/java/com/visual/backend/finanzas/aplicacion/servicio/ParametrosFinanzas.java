package com.visual.backend.finanzas.aplicacion.servicio;

import java.math.BigDecimal;

/**
 * Parametros configurables del tablero (los define la infraestructura).
 * umbralSignificativo: monto desde el cual un movimiento se destaca;
 * diasRecientes: ventana de los movimientos destacados.
 */
public record ParametrosFinanzas(BigDecimal umbralSignificativo, int diasRecientes) {
}
