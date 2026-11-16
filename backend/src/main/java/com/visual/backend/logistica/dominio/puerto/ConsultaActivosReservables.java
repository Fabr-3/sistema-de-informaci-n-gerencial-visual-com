package com.visual.backend.logistica.dominio.puerto;

import java.util.Collection;
import java.util.List;

import com.visual.backend.logistica.dominio.modelo.ActivoReservable;
import com.visual.backend.logistica.dominio.modelo.Filtros;
import com.visual.backend.logistica.dominio.modelo.PeriodoReserva;

/** Lectura de los activos del modulo de activos desde la logistica. */
public interface ConsultaActivosReservables {

    List<ActivoReservable> buscarPorIds(Collection<Integer> idsActivos);

    /** Equipos reservables sin reservas activas que se superpongan con el periodo. */
    List<ActivoReservable> buscarDisponibles(PeriodoReserva periodo, Filtros.ActivosReservables filtro);
}
