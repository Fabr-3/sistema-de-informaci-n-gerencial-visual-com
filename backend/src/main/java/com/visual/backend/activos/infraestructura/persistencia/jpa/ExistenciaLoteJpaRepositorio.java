package com.visual.backend.activos.infraestructura.persistencia.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import com.visual.backend.activos.infraestructura.persistencia.entidad.ExistenciaLoteEntidad;

public interface ExistenciaLoteJpaRepositorio extends JpaRepository<ExistenciaLoteEntidad, Integer>,
        JpaSpecificationExecutor<ExistenciaLoteEntidad> {

    /** SELECT ... FOR UPDATE: serializa los movimientos sobre la misma existencia. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ExistenciaLoteEntidad> findByModeloIdAndBodegaId(Integer idModelo, Integer idBodega);

    boolean existsByModeloId(Integer idModelo);

    @Query("""
            select e.modelo.id as idModelo, sum(e.cantidad) as cantidad from ExistenciaLoteEntidad e
            group by e.modelo.id
            """)
    List<ActivoJpaRepositorio.ConteoPorModelo> sumarPorModelo();

    @Override
    @EntityGraph(attributePaths = {"modelo", "modelo.fabricante", "modelo.categoria", "bodega"})
    List<ExistenciaLoteEntidad> findAll(Specification<ExistenciaLoteEntidad> especificacion,
                                        Sort orden);
}
