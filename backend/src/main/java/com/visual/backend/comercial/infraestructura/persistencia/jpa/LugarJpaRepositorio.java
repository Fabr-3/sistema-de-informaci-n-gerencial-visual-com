package com.visual.backend.comercial.infraestructura.persistencia.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.visual.backend.comercial.infraestructura.persistencia.entidad.LugarEntidad;

public interface LugarJpaRepositorio extends JpaRepository<LugarEntidad, Integer> {

    List<LugarEntidad> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCaseAndDireccionIgnoreCase(String nombre, String direccion);

    boolean existsByNombreIgnoreCaseAndDireccionIgnoreCaseAndIdNot(String nombre, String direccion, Integer id);
}
