package com.visual.backend.accesos.infraestructura.persistencia.adaptador;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.visual.backend.accesos.dominio.modelo.CodigoRol;
import com.visual.backend.accesos.dominio.modelo.Permiso;
import com.visual.backend.accesos.dominio.modelo.Rol;
import com.visual.backend.accesos.dominio.puerto.RepositorioRoles;
import com.visual.backend.accesos.infraestructura.persistencia.entidad.RolEntidad;
import com.visual.backend.accesos.infraestructura.persistencia.jpa.PermisoJpaRepositorio;
import com.visual.backend.accesos.infraestructura.persistencia.jpa.RolJpaRepositorio;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;

@Repository
public class RepositorioRolesJpa implements RepositorioRoles {

    private final RolJpaRepositorio roles;
    private final PermisoJpaRepositorio permisos;

    public RepositorioRolesJpa(RolJpaRepositorio roles, PermisoJpaRepositorio permisos) {
        this.roles = roles;
        this.permisos = permisos;
    }

    @Override
    public List<Rol> listar() {
        return roles.findAllByOrderByIdAsc().stream().map(MapeadorAccesos::aDominio).toList();
    }

    @Override
    public Optional<Rol> buscarPorId(Integer id) {
        return roles.findWithPermisosById(id).map(MapeadorAccesos::aDominio);
    }

    @Override
    public Optional<Rol> buscarPorCodigo(CodigoRol codigo) {
        return roles.findByCodigo(codigo).map(MapeadorAccesos::aDominio);
    }

    @Override
    public Rol guardarPermisos(Rol rol) {
        RolEntidad entidad = roles.findWithPermisosById(rol.getId())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el rol", rol.getId()));
        List<Integer> idsPermisos = rol.getPermisos().stream().map(Permiso::id).toList();
        entidad.setPermisos(new HashSet<>(permisos.findAllById(idsPermisos)));
        return MapeadorAccesos.aDominio(roles.save(entidad));
    }
}
