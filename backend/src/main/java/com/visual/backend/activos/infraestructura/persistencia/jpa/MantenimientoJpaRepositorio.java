package com.visual.backend.activos.infraestructura.persistencia.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.visual.backend.activos.infraestructura.persistencia.entidad.MantenimientoEntidad;

public interface MantenimientoJpaRepositorio extends JpaRepository<MantenimientoEntidad, Integer> {

    @EntityGraph(attributePaths = "tipo")
    Page<MantenimientoEntidad> findByIdActivoAndFechaEliminacionIsNull(Integer idActivo, Pageable paginacion);

    @EntityGraph(attributePaths = "tipo")
    Optional<MantenimientoEntidad> findByIdAndFechaEliminacionIsNull(Integer id);

    boolean existsByIdActivo(Integer idActivo);
}
