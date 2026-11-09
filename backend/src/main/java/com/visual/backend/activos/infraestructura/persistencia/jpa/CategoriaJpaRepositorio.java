package com.visual.backend.activos.infraestructura.persistencia.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.visual.backend.activos.infraestructura.persistencia.entidad.CategoriaEntidad;

public interface CategoriaJpaRepositorio extends JpaRepository<CategoriaEntidad, Integer> {

    List<CategoriaEntidad> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);
}
