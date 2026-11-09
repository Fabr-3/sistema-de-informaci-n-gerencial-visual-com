package com.visual.backend.accesos.infraestructura.persistencia.adaptador;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.visual.backend.accesos.dominio.modelo.Permiso;
import com.visual.backend.accesos.dominio.puerto.RepositorioPermisos;
import com.visual.backend.accesos.infraestructura.persistencia.jpa.PermisoJpaRepositorio;

@Repository
public class RepositorioPermisosJpa implements RepositorioPermisos {

    private final PermisoJpaRepositorio permisos;

    public RepositorioPermisosJpa(PermisoJpaRepositorio permisos) {
        this.permisos = permisos;
    }

    @Override
    public List<Permiso> listar() {
        return permisos.findAllByOrderByModuloAscNombreAsc().stream().map(MapeadorAccesos::aDominio).toList();
    }

    @Override
    public List<Permiso> buscarPorIds(Collection<Integer> ids) {
        return permisos.findAllById(ids).stream().map(MapeadorAccesos::aDominio).toList();
    }
}
