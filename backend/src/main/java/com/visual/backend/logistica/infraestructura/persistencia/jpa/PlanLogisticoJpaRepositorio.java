package com.visual.backend.logistica.infraestructura.persistencia.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.visual.backend.logistica.infraestructura.persistencia.entidad.PlanLogisticoEntidad;

public interface PlanLogisticoJpaRepositorio extends JpaRepository<PlanLogisticoEntidad, Integer> {

    Optional<PlanLogisticoEntidad> findByIdEventoAndFechaEliminacionIsNull(Integer idEvento);

    /** Avance de la planificacion de cada evento indicado (solo los que tienen plan). */
    @Query(nativeQuery = true, value = """
            select p.id_evento as idEvento,
                   (select count(*) from jornadas_logisticas j where j.id_plan = p.id_plan) as jornadas,
                   (select count(distinct a.id_personal) from asignaciones_personal a
                     where a.id_plan = p.id_plan) as personal,
                   (select count(*) from asignaciones_personal a
                     where a.id_plan = p.id_plan and exists (
                       select 1 from asignaciones_personal o
                       join planes_logisticos op on op.id_plan = o.id_plan and op.fecha_eliminacion is null
                       join eventos oe on oe.id_evento = op.id_evento
                       where o.id_personal = a.id_personal and o.id_plan <> a.id_plan
                         and oe.estado <> 'CANCELADO' and oe.fecha_eliminacion is null
                         and o.fecha_inicio_actividad < a.fecha_fin_actividad
                         and o.fecha_fin_actividad > a.fecha_inicio_actividad)) as cruces
            from planes_logisticos p
            where p.fecha_eliminacion is null and p.id_evento in (:idsEventos)
            """)
    List<AvanceLogistico> avance(@Param("idsEventos") Collection<Integer> idsEventos);

    interface AvanceLogistico {
        Integer getIdEvento();

        Long getJornadas();

        Long getPersonal();

        Long getCruces();
    }
}
