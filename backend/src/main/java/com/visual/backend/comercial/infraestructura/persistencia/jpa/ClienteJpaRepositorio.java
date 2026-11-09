package com.visual.backend.comercial.infraestructura.persistencia.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.visual.backend.comercial.infraestructura.persistencia.entidad.ClienteEntidad;

public interface ClienteJpaRepositorio extends JpaRepository<ClienteEntidad, Integer>,
        JpaSpecificationExecutor<ClienteEntidad> {

    @EntityGraph(attributePaths = "tipo")
    Optional<ClienteEntidad> findByIdAndFechaEliminacionIsNull(Integer id);

    @Override
    @EntityGraph(attributePaths = "tipo")
    Page<ClienteEntidad> findAll(Specification<ClienteEntidad> especificacion, Pageable paginacion);

    /** Documento ya registrado en otro cliente (los de clientes eliminados tambien cuentan: es unico en la base). */
    @Query("""
            select count(d) > 0 from DocumentoClienteEntidad d
            where d.tipo.id = :idTipo and upper(d.numero) = upper(:numero)
              and (:idExcluido is null or d.cliente.id <> :idExcluido)
            """)
    boolean existeDocumento(@Param("idTipo") Integer idTipo, @Param("numero") String numero,
                            @Param("idExcluido") Integer idExcluido);
}
