package com.visual.backend.finanzas.infraestructura.persistencia.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.visual.backend.finanzas.infraestructura.persistencia.entidad.MovimientoFinancieroEntidad;

public interface MovimientoFinancieroJpaRepositorio extends JpaRepository<MovimientoFinancieroEntidad, Integer>,
        JpaSpecificationExecutor<MovimientoFinancieroEntidad> {
}
