package com.visual.backend.logistica.infraestructura.persistencia.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.visual.backend.logistica.infraestructura.persistencia.entidad.EspecialidadEntidad;

public interface EspecialidadJpaRepositorio extends JpaRepository<EspecialidadEntidad, Integer> {
}
