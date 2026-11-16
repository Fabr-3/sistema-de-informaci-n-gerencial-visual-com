package com.visual.backend.activos.dominio.puerto;

import java.time.LocalDateTime;
import java.util.List;

import com.visual.backend.activos.dominio.modelo.ReservaVigenteActivo;

/** Lectura de las reservas de equipos del modulo de logistica, sin depender de el. */
public interface ConsultaReservasActivo {

    /** Reservas activas del equipo cuyo retorno es posterior al instante indicado. */
    List<ReservaVigenteActivo> vigentes(Integer idActivo, LocalDateTime desde);
}
