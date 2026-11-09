package com.visual.backend.accesos.infraestructura.persistencia.jpa;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.visual.backend.accesos.dominio.modelo.CodigoRol;
import com.visual.backend.accesos.infraestructura.persistencia.entidad.UsuarioEntidad;

public interface UsuarioJpaRepositorio extends JpaRepository<UsuarioEntidad, Integer>,
        JpaSpecificationExecutor<UsuarioEntidad> {

    @EntityGraph(attributePaths = "rol")
    Optional<UsuarioEntidad> findByIdAndFechaEliminacionIsNull(Integer id);

    @EntityGraph(attributePaths = "rol")
    Optional<UsuarioEntidad> findByNombreUsuarioAndFechaEliminacionIsNull(String nombreUsuario);

    /** Incluye eliminados: el indice unico de la base de datos tambien los incluye. */
    boolean existsByNombreUsuario(String nombreUsuario);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);

    boolean existsByRolCodigoAndFechaEliminacionIsNull(CodigoRol codigo);

    @Query("""
            select count(u) from UsuarioEntidad u
            where u.rol.codigo = :codigo
              and u.activo = true
              and u.fechaEliminacion is null
              and (u.fechaExpiracion is null or u.fechaExpiracion > :ahora)
              and u.id <> :idExcluido
            """)
    long contarHabilitadosPorRolExcluyendo(@Param("codigo") CodigoRol codigo,
                                           @Param("idExcluido") Integer idExcluido,
                                           @Param("ahora") LocalDateTime ahora);

    @Override
    @EntityGraph(attributePaths = "rol")
    Page<UsuarioEntidad> findAll(Specification<UsuarioEntidad> especificacion, Pageable paginacion);
}
