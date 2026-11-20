package com.visual.backend.finanzas.dominio.puerto;

import java.util.List;
import java.util.Optional;

import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.compartido.dominio.Pagina;
import com.visual.backend.compartido.dominio.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.finanzas.dominio.modelo.Filtros;
import com.visual.backend.finanzas.dominio.modelo.Proveedor;

public interface RepositorioProveedores {

    Optional<Proveedor> buscarPorId(Integer id);

    default Proveedor obtener(Integer id) {
        return buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("el proveedor", id));
    }

    Pagina<Proveedor> buscar(Filtros.Proveedores filtro, SolicitudPagina solicitud);

    /** Documento (tipo, numero y complemento) ya registrado en otro proveedor. */
    boolean existeDocumento(Integer idTipoDocumento, String numero, String complemento, Integer idExcluido);

    List<ElementoCatalogo> tiposProveedor();

    List<ElementoCatalogo> tiposDocumento();

    boolean tieneMovimientos(Integer idProveedor);

    Proveedor guardar(Proveedor proveedor);
}
