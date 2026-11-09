package com.visual.backend.comercial.infraestructura.persistencia.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.visual.backend.comercial.infraestructura.persistencia.entidad.ProyectoEntidad;

public interface ProyectoJpaRepositorio extends JpaRepository<ProyectoEntidad, Integer>,
        JpaSpecificationExecutor<ProyectoEntidad> {

    @EntityGraph(attributePaths = {"cotizacion", "cotizacion.cliente"})
    Optional<ProyectoEntidad> findByIdAndFechaEliminacionIsNull(Integer id);

    @EntityGraph(attributePaths = {"cotizacion", "cotizacion.cliente"})
    Optional<ProyectoEntidad> findByCotizacionIdAndFechaEliminacionIsNull(Integer idCotizacion);

    @Override
    @EntityGraph(attributePaths = {"cotizacion", "cotizacion.cliente"})
    Page<ProyectoEntidad> findAll(Specification<ProyectoEntidad> especificacion, Pageable paginacion);

    @Query("""
            select p.cotizacion.id as idCotizacion, p.id as idProyecto from ProyectoEntidad p
            where p.cotizacion.id in :ids and p.fechaEliminacion is null
            """)
    List<ProyectoPorCotizacion> buscarIdsPorCotizacion(@Param("ids") Collection<Integer> idsCotizaciones);

    interface ProyectoPorCotizacion {

        Integer getIdCotizacion();

        Integer getIdProyecto();
    }
}
