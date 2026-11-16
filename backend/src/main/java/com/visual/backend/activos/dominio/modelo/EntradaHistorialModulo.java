package com.visual.backend.activos.dominio.modelo;

import java.time.LocalDateTime;

/** Linea del historial de un modulo LED con los codigos y el usuario para mostrarla. */
public record EntradaHistorialModulo(TipoCambioModulo tipo, String pantallaOrigen, String pantallaDestino,
                                     EstadoModuloLed estadoAnterior, EstadoModuloLed estadoNuevo,
                                     String moduloRelacionado, String motivo, LocalDateTime fecha,
                                     String registradoPor) {
}
