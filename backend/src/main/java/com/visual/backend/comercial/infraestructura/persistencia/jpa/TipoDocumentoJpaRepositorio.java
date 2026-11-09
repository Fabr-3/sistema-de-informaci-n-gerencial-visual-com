package com.visual.backend.comercial.infraestructura.persistencia.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.visual.backend.comercial.infraestructura.persistencia.entidad.TipoDocumentoEntidad;

public interface TipoDocumentoJpaRepositorio extends JpaRepository<TipoDocumentoEntidad, Integer> {

    List<TipoDocumentoEntidad> findAllByOrderByNombreAsc();
}
