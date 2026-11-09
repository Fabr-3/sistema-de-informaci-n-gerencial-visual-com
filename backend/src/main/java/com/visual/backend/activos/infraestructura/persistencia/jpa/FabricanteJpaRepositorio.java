package com.visual.backend.activos.infraestructura.persistencia.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.visual.backend.activos.infraestructura.persistencia.entidad.FabricanteEntidad;

public interface FabricanteJpaRepositorio extends JpaRepository<FabricanteEntidad, Integer> {

    List<FabricanteEntidad> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);
}
