package com.visual.backend.activos.infraestructura.persistencia.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.visual.backend.activos.infraestructura.persistencia.entidad.MovimientoLoteEntidad;

public interface MovimientoLoteJpaRepositorio extends JpaRepository<MovimientoLoteEntidad, Integer> {

    @EntityGraph(attributePaths = "creador")
    Page<MovimientoLoteEntidad> findByIdExistencia(Integer idExistencia, Pageable paginacion);
}
