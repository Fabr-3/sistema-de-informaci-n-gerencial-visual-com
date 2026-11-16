package com.visual.backend.comercial.aplicacion.evento;

import java.time.LocalDateTime;

/**
 * Se publica dentro de la transaccion que cambia el horario de un evento,
 * para que otros modulos (logistica) ajusten lo que dependa de el.
 */
public record EventoReprogramado(Integer idEvento, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
}
