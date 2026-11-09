package com.visual.backend.activos.infraestructura.persistencia.jpa;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.visual.backend.activos.infraestructura.persistencia.entidad.RegistroUsoEntidad;

public interface RegistroUsoJpaRepositorio extends JpaRepository<RegistroUsoEntidad, Integer> {

    @EntityGraph(attributePaths = "creador")
    Page<RegistroUsoEntidad> findByIdActivoAndFechaEliminacionIsNull(Integer idActivo, Pageable paginacion);

    Optional<RegistroUsoEntidad> findByIdAndFechaEliminacionIsNull(Integer id);

    boolean existsByIdActivo(Integer idActivo);

    /** Superposicion de intervalos semiabiertos [inicio, fin). */
    @Query("""
            select count(r) > 0 from RegistroUsoEntidad r
            where r.idActivo = :idActivo and r.fechaEliminacion is null
              and r.fechaInicio < :fin and r.fechaFin > :inicio
            """)
    boolean existeSuperposicion(@Param("idActivo") Integer idActivo, @Param("inicio") LocalDateTime inicio,
                                @Param("fin") LocalDateTime fin);
}
