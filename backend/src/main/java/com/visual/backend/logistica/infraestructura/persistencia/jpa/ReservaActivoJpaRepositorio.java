package com.visual.backend.logistica.infraestructura.persistencia.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.visual.backend.logistica.dominio.modelo.EstadoReserva;
import com.visual.backend.logistica.infraestructura.persistencia.entidad.ReservaActivoEntidad;

public interface ReservaActivoJpaRepositorio extends JpaRepository<ReservaActivoEntidad, Integer> {

    List<ReservaActivoEntidad> findByIdPlanAndEstadoOrderByIdAsc(Integer idPlan, EstadoReserva estado);
}
