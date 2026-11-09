package com.visual.backend.activos.infraestructura.persistencia.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.visual.backend.activos.infraestructura.persistencia.entidad.BodegaEntidad;

public interface BodegaJpaRepositorio extends JpaRepository<BodegaEntidad, Integer> {

    List<BodegaEntidad> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);
}
