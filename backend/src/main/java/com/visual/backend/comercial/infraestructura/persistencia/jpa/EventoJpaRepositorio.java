package com.visual.backend.comercial.infraestructura.persistencia.jpa;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.visual.backend.comercial.dominio.modelo.EstadoEvento;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.EventoEntidad;

public interface EventoJpaRepositorio extends JpaRepository<EventoEntidad, Integer> {

    @EntityGraph(attributePaths = {"lugar", "proyecto"})
    Optional<EventoEntidad> findByIdAndFechaEliminacionIsNull(Integer id);

    @EntityGraph(attributePaths = {"lugar", "proyecto"})
    List<EventoEntidad> findByProyectoIdAndFechaEliminacionIsNullOrderByFechaInicioAsc(Integer idProyecto);

    /** Eventos que se cruzan con [desde, hasta), opcionalmente de un proyecto. */
    @EntityGraph(attributePaths = {"lugar", "proyecto", "proyecto.cotizacion", "proyecto.cotizacion.cliente"})
    @Query("""
            select e from EventoEntidad e
            where e.fechaEliminacion is null and e.fechaInicio < :hasta and e.fechaFin > :desde
              and (:idProyecto is null or e.proyecto.id = :idProyecto)
            order by e.fechaInicio
            """)
    List<EventoEntidad> buscarEnRango(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                                      @Param("idProyecto") Integer idProyecto);

    /** Los limites nulos se reemplazan por fechas extremas en el adaptador. */
    @Query("""
            select count(e) > 0 from EventoEntidad e
            where e.proyecto.id = :idProyecto and e.fechaEliminacion is null and e.estado <> :cancelado
              and (cast(e.fechaInicio as LocalDate) < :inicio or cast(e.fechaFin as LocalDate) > :fin)
            """)
    boolean existenFueraDe(@Param("idProyecto") Integer idProyecto, @Param("inicio") LocalDate inicio,
                           @Param("fin") LocalDate fin, @Param("cancelado") EstadoEvento cancelado);
}
