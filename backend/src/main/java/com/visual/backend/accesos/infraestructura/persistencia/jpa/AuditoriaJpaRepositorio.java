package com.visual.backend.accesos.infraestructura.persistencia.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.visual.backend.accesos.infraestructura.persistencia.entidad.AuditoriaEntidad;

public interface AuditoriaJpaRepositorio extends JpaRepository<AuditoriaEntidad, Long>,
        JpaSpecificationExecutor<AuditoriaEntidad> {
}
