package com.visual.backend.activos.infraestructura.persistencia.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.visual.backend.activos.dominio.modelo.EstadoOperativo;
import com.visual.backend.activos.infraestructura.persistencia.entidad.ActivoEntidad;

public interface ActivoJpaRepositorio extends JpaRepository<ActivoEntidad, Integer>,
        JpaSpecificationExecutor<ActivoEntidad> {

    @EntityGraph(attributePaths = {"modelo", "modelo.fabricante", "modelo.categoria", "bodega"})
    Optional<ActivoEntidad> findByIdAndFechaEliminacionIsNull(Integer id);

    /** Incluye eliminados: el indice unico de la base de datos tambien los incluye. */
    boolean existsByCodigoInventario(String codigoInventario);

    boolean existsByModeloIdAndNumeroSerieIgnoreCase(Integer idModelo, String numeroSerie);

    boolean existsByModeloIdAndNumeroSerieIgnoreCaseAndIdNot(Integer idModelo, String numeroSerie, Integer id);

    boolean existsByModeloId(Integer idModelo);

    @Query("""
            select a.modelo.id as idModelo, count(a) as cantidad from ActivoEntidad a
            where a.fechaEliminacion is null and a.estado <> :excluido
            group by a.modelo.id
            """)
    List<ConteoPorModelo> contarPorModelo(@Param("excluido") EstadoOperativo excluido);

    interface ConteoPorModelo {

        Integer getIdModelo();

        Long getCantidad();
    }

    @Override
    @EntityGraph(attributePaths = {"modelo", "modelo.fabricante", "modelo.categoria", "bodega"})
    Page<ActivoEntidad> findAll(Specification<ActivoEntidad> especificacion, Pageable paginacion);

    @Override
    @EntityGraph(attributePaths = {"modelo", "modelo.fabricante", "modelo.categoria", "bodega"})
    List<ActivoEntidad> findAll(Specification<ActivoEntidad> especificacion);
}
