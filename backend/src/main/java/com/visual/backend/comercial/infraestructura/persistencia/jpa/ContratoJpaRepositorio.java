package com.visual.backend.comercial.infraestructura.persistencia.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.visual.backend.comercial.infraestructura.persistencia.entidad.ContratoEntidad;

public interface ContratoJpaRepositorio extends JpaRepository<ContratoEntidad, Integer> {

    List<ContratoEntidad> findByIdProyectoAndFechaEliminacionIsNullOrderByFechaFirmaDesc(Integer idProyecto);

    Optional<ContratoEntidad> findByIdAndFechaEliminacionIsNull(Integer id);
}
