package com.visual.backend.logistica.infraestructura.persistencia.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.visual.backend.comercial.infraestructura.persistencia.entidad.EventoEntidad;

/** Busqueda de eventos comerciales con los filtros propios de la logistica. */
public interface EventoLogisticaJpaRepositorio extends JpaRepository<EventoEntidad, Integer>,
        JpaSpecificationExecutor<EventoEntidad> {

    @Query(nativeQuery = true, value = """
            select exists (select 1 from proyectos p where p.id_proyecto = :idProyecto
                           and p.fecha_eliminacion is null and p.estado in ('PLANIFICACION', 'EN_EJECUCION'))
            """)
    boolean proyectoVigente(@Param("idProyecto") Integer idProyecto);
}
