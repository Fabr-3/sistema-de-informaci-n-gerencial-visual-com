package com.visual.backend.comercial.dominio.puerto;

import java.util.Optional;

import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;

import com.visual.backend.comercial.dominio.modelo.Cliente;
import com.visual.backend.comercial.dominio.modelo.Filtros;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;

/** Persistencia de clientes. Las consultas excluyen los eliminados. */
public interface RepositorioClientes {

    Optional<Cliente> buscarPorId(Integer id);

    /** Cliente vigente o excepcion de recurso no encontrado. */
    default Cliente obtener(Integer id) {
        return buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("el cliente", id));
    }

    Pagina<Cliente> buscar(Filtros.Clientes filtro, SolicitudPagina solicitud);

    /** Indica si el documento ya pertenece a otro cliente distinto de idExcluido (puede ser nulo). */
    boolean existeDocumento(Integer idTipoDocumento, String numero, Integer idExcluido);

    Cliente guardar(Cliente cliente);
}
