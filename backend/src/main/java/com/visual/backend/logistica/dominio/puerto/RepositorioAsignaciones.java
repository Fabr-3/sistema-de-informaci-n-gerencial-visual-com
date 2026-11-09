package com.visual.backend.logistica.dominio.puerto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.logistica.dominio.modelo.Asignacion;
import com.visual.backend.logistica.dominio.modelo.AsignacionPropia;
import com.visual.backend.logistica.dominio.modelo.Cruce;
import com.visual.backend.logistica.dominio.modelo.Periodo;

public interface RepositorioAsignaciones {

    Optional<Asignacion> buscarPorId(Integer id);

    default Asignacion obtener(Integer id) {
        return buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("la asignación", id));
    }

    List<Asignacion> listarPorPlan(Integer idPlan);

    /** La persona ya trabaja en ese horario dentro del mismo plan. */
    boolean existeEnPlan(Integer idPlan, Integer idPersonal, Periodo periodo, Integer idExcluido);

    /** Alguna asignacion del plan queda fuera del periodo indicado. */
    boolean existenFueraDe(Integer idPlan, Periodo periodo);

    /**
     * Asignaciones en otros planes, de eventos no cancelados, que se superponen
     * con el periodo (idPersonal nulo = de todo el personal).
     */
    List<Cruce> cruces(Integer idPersonal, Periodo periodo, Integer idPlanExcluido);

    /** Asignaciones vigentes (que terminan despues de "desde") de eventos no cancelados. */
    List<AsignacionPropia> listarDePersonal(Integer idPersonal, LocalDateTime desde);

    Asignacion guardar(Asignacion asignacion);

    void eliminar(Integer id);
}
