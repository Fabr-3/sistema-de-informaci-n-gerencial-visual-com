package com.visual.backend.activos.dominio.modelo;

import java.time.LocalDateTime;

/** Reserva activa de un equipo para un evento que aun no termina (la registra logistica). */
public record ReservaVigenteActivo(Integer idReserva, Integer idEvento, String evento, LocalDateTime salida,
                                   LocalDateTime retorno) {
}
