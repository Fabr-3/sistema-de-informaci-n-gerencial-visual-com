package com.visual.backend.comercial.infraestructura.persistencia.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.visual.backend.comercial.infraestructura.persistencia.entidad.TipoClienteEntidad;

public interface TipoClienteJpaRepositorio extends JpaRepository<TipoClienteEntidad, Integer> {

    List<TipoClienteEntidad> findAllByOrderByNombreAsc();
}
