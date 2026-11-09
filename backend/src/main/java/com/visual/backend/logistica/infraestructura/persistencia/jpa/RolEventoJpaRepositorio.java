package com.visual.backend.logistica.infraestructura.persistencia.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.visual.backend.logistica.infraestructura.persistencia.entidad.RolEventoEntidad;

public interface RolEventoJpaRepositorio extends JpaRepository<RolEventoEntidad, Integer> {
}
