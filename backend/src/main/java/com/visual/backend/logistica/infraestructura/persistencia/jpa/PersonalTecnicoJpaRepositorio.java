package com.visual.backend.logistica.infraestructura.persistencia.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.visual.backend.logistica.infraestructura.persistencia.entidad.PersonalTecnicoEntidad;

public interface PersonalTecnicoJpaRepositorio extends JpaRepository<PersonalTecnicoEntidad, Integer>,
        JpaSpecificationExecutor<PersonalTecnicoEntidad> {

    Optional<PersonalTecnicoEntidad> findByIdAndFechaEliminacionIsNull(Integer id);

    List<PersonalTecnicoEntidad> findByActivoTrueAndFechaEliminacionIsNullOrderByNombresAscApellidosAsc();

    Optional<PersonalTecnicoEntidad> findByUsuarioIdAndFechaEliminacionIsNull(Integer idUsuario);

    @Query("""
            select count(d) > 0 from DocumentoPersonalEntidad d
            where d.tipo.id = :idTipo and upper(d.numero) = upper(:numero) and d.fechaEliminacion is null
              and d.personal.fechaEliminacion is null and (:idExcluido is null or d.personal.id <> :idExcluido)
            """)
    boolean existeDocumento(@Param("idTipo") Integer idTipo, @Param("numero") String numero,
                            @Param("idExcluido") Integer idExcluido);

    @Query("""
            select count(p) > 0 from PersonalTecnicoEntidad p
            where p.usuario.id = :idUsuario and p.fechaEliminacion is null
              and (:idExcluido is null or p.id <> :idExcluido)
            """)
    boolean usuarioVinculadoAOtro(@Param("idUsuario") Integer idUsuario, @Param("idExcluido") Integer idExcluido);
}
