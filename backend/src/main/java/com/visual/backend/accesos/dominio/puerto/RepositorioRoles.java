package com.visual.backend.accesos.dominio.puerto;

import java.util.List;
import java.util.Optional;

import com.visual.backend.accesos.dominio.modelo.CodigoRol;
import com.visual.backend.accesos.dominio.modelo.Rol;

/** Persistencia de roles (catalogo fijo; solo cambian sus permisos). */
public interface RepositorioRoles {

    List<Rol> listar();

    Optional<Rol> buscarPorId(Integer id);

    Optional<Rol> buscarPorCodigo(CodigoRol codigo);

    Rol guardarPermisos(Rol rol);
}
