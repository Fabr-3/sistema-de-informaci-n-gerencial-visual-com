package com.visual.backend.logistica.dominio.puerto;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.visual.backend.logistica.dominio.modelo.ActivoReservable;
import com.visual.backend.logistica.dominio.modelo.DisponibilidadModelo;
import com.visual.backend.logistica.dominio.modelo.Filtros;
import com.visual.backend.logistica.dominio.modelo.PeriodoReserva;

/** Lectura de los activos del modulo de activos desde la logistica. */
public interface ConsultaActivosReservables {

    List<ActivoReservable> buscarPorIds(Collection<Integer> idsActivos);

    /** Equipos reservables sin reservas activas que se superpongan con el periodo. */
    List<ActivoReservable> buscarDisponibles(PeriodoReserva periodo, Filtros.ActivosReservables filtro);

    /** Unidades libres por modelo (solo modelos con al menos una unidad libre). */
    List<DisponibilidadModelo> disponibilidadPorModelo(PeriodoReserva periodo);

    /** Las primeras unidades libres del modelo, por codigo de inventario. */
    List<ActivoReservable> elegirDisponibles(PeriodoReserva periodo, Integer idModelo, int cantidad);

    /** Nombre del modelo (fabricante y modelo) para los mensajes. */
    Optional<String> nombreModelo(Integer idModelo);
}
