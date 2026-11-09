package com.visual.backend.accesos.dominio.puerto;

import java.util.Collection;
import java.util.List;

import com.visual.backend.accesos.dominio.modelo.Permiso;

/** Consulta del catalogo fijo de permisos. */
public interface RepositorioPermisos {

    List<Permiso> listar();

    List<Permiso> buscarPorIds(Collection<Integer> ids);
}
