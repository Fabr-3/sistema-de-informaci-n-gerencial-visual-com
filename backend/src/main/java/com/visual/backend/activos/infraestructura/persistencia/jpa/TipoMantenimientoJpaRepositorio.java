package com.visual.backend.activos.infraestructura.persistencia.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.visual.backend.activos.infraestructura.persistencia.entidad.TipoMantenimientoEntidad;

public interface TipoMantenimientoJpaRepositorio extends JpaRepository<TipoMantenimientoEntidad, Integer> {

    List<TipoMantenimientoEntidad> findAllByOrderByNombreAsc();
}
