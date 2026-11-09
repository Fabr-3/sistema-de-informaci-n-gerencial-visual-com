package com.visual.backend.logistica.dominio.modelo;

import java.time.LocalDateTime;

/** Otra asignacion de la misma persona que se superpone en el tiempo (solo se alerta). */
public record Cruce(Integer idAsignacion, Integer idPersonal, Integer idEvento, String evento, LocalDateTime inicio,
                    LocalDateTime fin) {
}
