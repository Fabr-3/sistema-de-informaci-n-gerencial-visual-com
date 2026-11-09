package com.visual.backend.comercial.infraestructura.persistencia.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.visual.backend.comercial.infraestructura.persistencia.entidad.TipoContactoEntidad;

public interface TipoContactoJpaRepositorio extends JpaRepository<TipoContactoEntidad, Integer> {

    List<TipoContactoEntidad> findAllByOrderByNombreAsc();
}
