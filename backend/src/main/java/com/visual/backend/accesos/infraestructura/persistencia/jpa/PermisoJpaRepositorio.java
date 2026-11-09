package com.visual.backend.accesos.infraestructura.persistencia.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.visual.backend.accesos.infraestructura.persistencia.entidad.PermisoEntidad;

public interface PermisoJpaRepositorio extends JpaRepository<PermisoEntidad, Integer> {

    List<PermisoEntidad> findAllByOrderByModuloAscNombreAsc();
}
