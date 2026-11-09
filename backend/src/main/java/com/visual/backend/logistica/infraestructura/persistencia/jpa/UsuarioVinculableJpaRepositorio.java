package com.visual.backend.logistica.infraestructura.persistencia.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.visual.backend.accesos.dominio.modelo.CodigoRol;
import com.visual.backend.accesos.infraestructura.persistencia.entidad.UsuarioEntidad;

/** Lectura de las cuentas que se pueden vincular al personal tecnico. */
public interface UsuarioVinculableJpaRepositorio extends Repository<UsuarioEntidad, Integer> {

    @Query("""
            select u from UsuarioEntidad u
            where u.fechaEliminacion is null and u.activo = true and u.rol.codigo in :roles
              and not exists (select p.id from PersonalTecnicoEntidad p
                              where p.usuario = u and p.fechaEliminacion is null)
            order by u.nombres, u.apellidoPaterno
            """)
    List<UsuarioEntidad> listar(@Param("roles") Collection<CodigoRol> roles);

    @Query("""
            select u from UsuarioEntidad u
            where u.id = :id and u.fechaEliminacion is null and u.activo = true and u.rol.codigo in :roles
            """)
    Optional<UsuarioEntidad> buscar(@Param("id") Integer id, @Param("roles") Collection<CodigoRol> roles);
}
