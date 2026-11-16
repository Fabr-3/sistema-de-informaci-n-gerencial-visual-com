package com.visual.backend.logistica.dominio.modelo;

import java.time.LocalDateTime;

/** Reserva activa de otro evento que ocupa el activo en el periodo solicitado. */
public record ConflictoReserva(Integer idActivo, String codigoActivo, Integer idEvento, String evento,
                               LocalDateTime salida, LocalDateTime retorno) {
}
