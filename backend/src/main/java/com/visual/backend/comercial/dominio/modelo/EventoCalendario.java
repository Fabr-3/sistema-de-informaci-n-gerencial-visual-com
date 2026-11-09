package com.visual.backend.comercial.dominio.modelo;

import java.time.LocalDateTime;

/** Modelo de lectura del cronograma: el evento con su proyecto, cliente y lugar. */
public record EventoCalendario(Integer id, String nombre, LocalDateTime fechaInicio, LocalDateTime fechaFin,
                               EstadoEvento estado, Integer idProyecto, String proyecto, String cliente,
                               String lugar, String ciudad) {
}
