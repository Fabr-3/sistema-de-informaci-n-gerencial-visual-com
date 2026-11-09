package com.visual.backend.logistica.infraestructura.persistencia.jpa;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.visual.backend.comercial.dominio.modelo.EstadoEvento;
import com.visual.backend.logistica.dominio.modelo.Cruce;
import com.visual.backend.logistica.infraestructura.persistencia.entidad.AsignacionEntidad;

public interface AsignacionJpaRepositorio extends JpaRepository<AsignacionEntidad, Integer> {

    List<AsignacionEntidad> findByIdPlanOrderByFechaInicioAscIdAsc(Integer idPlan);

    @Query("""
            select count(a) > 0 from AsignacionEntidad a
            where a.idPlan = :idPlan and a.personal.id = :idPersonal
              and (:idExcluido is null or a.id <> :idExcluido)
              and a.fechaInicio < :fin and a.fechaFin > :inicio
            """)
    boolean existeEnPlan(@Param("idPlan") Integer idPlan, @Param("idPersonal") Integer idPersonal,
                         @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin,
                         @Param("idExcluido") Integer idExcluido);

    @Query("""
            select count(a) > 0 from AsignacionEntidad a
            where a.idPlan = :idPlan and (a.fechaInicio < :inicio or a.fechaFin > :fin)
            """)
    boolean existenFueraDe(@Param("idPlan") Integer idPlan, @Param("inicio") LocalDateTime inicio,
                           @Param("fin") LocalDateTime fin);

    @Query("""
            select new com.visual.backend.logistica.dominio.modelo.Cruce(
                       a.id, a.personal.id, e.id, e.nombre, a.fechaInicio, a.fechaFin)
            from AsignacionEntidad a, PlanLogisticoEntidad p, EventoEntidad e
            where p.id = a.idPlan and e.id = p.idEvento and p.fechaEliminacion is null
              and e.fechaEliminacion is null and e.estado <> :cancelado
              and (:idPersonal is null or a.personal.id = :idPersonal)
              and (:idPlanExcluido is null or a.idPlan <> :idPlanExcluido)
              and a.fechaInicio < :fin and a.fechaFin > :inicio
            order by a.fechaInicio
            """)
    List<Cruce> cruces(@Param("idPersonal") Integer idPersonal, @Param("inicio") LocalDateTime inicio,
                       @Param("fin") LocalDateTime fin, @Param("idPlanExcluido") Integer idPlanExcluido,
                       @Param("cancelado") EstadoEvento cancelado);

    /** Filas [asignacion, detalles de transporte, evento] de la persona. */
    @Query("""
            select a, p.detallesTransporte, e
            from AsignacionEntidad a, PlanLogisticoEntidad p, EventoEntidad e
            where p.id = a.idPlan and e.id = p.idEvento and p.fechaEliminacion is null
              and e.fechaEliminacion is null and e.estado <> :cancelado
              and a.personal.id = :idPersonal and a.fechaFin > :desde
            order by a.fechaInicio
            """)
    List<Object[]> listarDePersonal(@Param("idPersonal") Integer idPersonal, @Param("desde") LocalDateTime desde,
                                    @Param("cancelado") EstadoEvento cancelado);
}
