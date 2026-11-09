package com.visual.backend.comercial.infraestructura.persistencia.jpa;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.visual.backend.comercial.dominio.modelo.EstadoCotizacion;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.CotizacionEntidad;

public interface CotizacionJpaRepositorio extends JpaRepository<CotizacionEntidad, Integer>,
        JpaSpecificationExecutor<CotizacionEntidad> {

    @EntityGraph(attributePaths = {"cliente", "resolutor"})
    Optional<CotizacionEntidad> findByIdAndFechaEliminacionIsNull(Integer id);

    @Override
    @EntityGraph(attributePaths = {"cliente", "resolutor"})
    Page<CotizacionEntidad> findAll(Specification<CotizacionEntidad> especificacion, Pageable paginacion);

    List<CotizacionEntidad> findByEstadoAndFechaValidezBeforeAndFechaEliminacionIsNull(EstadoCotizacion estado,
                                                                                      LocalDate fecha);

    /** Incluye eliminadas: el historial comercial del cliente se conserva. */
    boolean existsByClienteId(Integer idCliente);
}
