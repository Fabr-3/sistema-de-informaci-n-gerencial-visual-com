package com.visual.backend.logistica.dominio.puerto;

import java.util.List;
import java.util.Optional;

import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.logistica.dominio.modelo.ConflictoReserva;
import com.visual.backend.logistica.dominio.modelo.PeriodoReserva;
import com.visual.backend.logistica.dominio.modelo.ReservaActivo;

public interface RepositorioReservas {

    List<ReservaActivo> buscarActivasPorPlan(Integer idPlan);

    Optional<ReservaActivo> buscarPorId(Integer idReserva);

    default ReservaActivo obtener(Integer idReserva) {
        return buscarPorId(idReserva).orElseThrow(() -> new RecursoNoEncontradoExcepcion("la reserva", idReserva));
    }

    /** Reservas activas del activo que se superponen con el periodo, salvo la indicada (puede ser nula). */
    List<ConflictoReserva> buscarSuperpuestas(Integer idActivo, PeriodoReserva periodo, Integer idReservaExcluida);

    ReservaActivo guardar(ReservaActivo reserva);

    List<ReservaActivo> guardarTodas(List<ReservaActivo> reservas);
}
