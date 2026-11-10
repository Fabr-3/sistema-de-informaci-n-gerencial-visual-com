package com.visual.backend.finanzas.dominio.puerto;

import java.util.Optional;

import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.finanzas.dominio.modelo.Filtros;
import com.visual.backend.finanzas.dominio.modelo.Proveedor;

public interface RepositorioProveedores {

    Optional<Proveedor> buscarPorId(Integer id);

    default Proveedor obtener(Integer id) {
        return buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("el proveedor", id));
    }

    Pagina<Proveedor> buscar(Filtros.Proveedores filtro, SolicitudPagina solicitud);

    boolean existeNit(String nit, Integer idExcluido);

    boolean tieneMovimientos(Integer idProveedor);

    Proveedor guardar(Proveedor proveedor);
}
