package com.visual.backend.activos.infraestructura.persistencia.jpa;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.visual.backend.activos.infraestructura.persistencia.entidad.UsoActivoEntidad;

public interface UsoActivoJpaRepositorio extends JpaRepository<UsoActivoEntidad, Integer> {

    /**
     * Se consulta mediante proyeccion (no entidades administradas) para leer
     * siempre el valor actual de la vista, aun dentro de la misma transaccion.
     */
    List<ProyeccionUso> findByIdActivoIn(Collection<Integer> idsActivos);

    interface ProyeccionUso {

        Integer getIdActivo();

        BigDecimal getHorasTotales();

        BigDecimal getHorasDesdeMantenimiento();

        LocalDateTime getFechaUltimoMantenimiento();
    }
}
