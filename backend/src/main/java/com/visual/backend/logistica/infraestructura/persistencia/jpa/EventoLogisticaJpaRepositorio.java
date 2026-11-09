package com.visual.backend.logistica.infraestructura.persistencia.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.visual.backend.comercial.infraestructura.persistencia.entidad.EventoEntidad;

/** Busqueda de eventos comerciales con los filtros propios de la logistica. */
public interface EventoLogisticaJpaRepositorio extends JpaRepository<EventoEntidad, Integer>,
        JpaSpecificationExecutor<EventoEntidad> {
}
