package com.visual.backend.subcontratacion.dominio.puerto;

import java.util.Optional;

import com.visual.backend.compartido.dominio.Pagina;
import com.visual.backend.compartido.dominio.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.subcontratacion.dominio.modelo.Filtros;
import com.visual.backend.subcontratacion.dominio.modelo.Subcontratacion;

public interface RepositorioSubcontrataciones {

    Pagina<Subcontratacion> buscar(Filtros.Subcontrataciones filtro, SolicitudPagina solicitud);

    Optional<Subcontratacion> buscarPorId(Integer id);

    default Subcontratacion obtener(Integer id) {
        return buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("la subcontratación", id));
    }

    Subcontratacion guardar(Subcontratacion subcontratacion);
}
