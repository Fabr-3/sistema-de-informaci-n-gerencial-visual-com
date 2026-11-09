package com.visual.backend.comercial.infraestructura.persistencia.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.visual.backend.comercial.infraestructura.persistencia.entidad.ServicioEntidad;

public interface ServicioJpaRepositorio extends JpaRepository<ServicioEntidad, Integer> {

    List<ServicioEntidad> findAllByOrderByNombreAsc();

    List<ServicioEntidad> findByActivoTrueOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);
}
