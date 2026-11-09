package com.visual.backend.comercial.dominio.puerto;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;

import com.visual.backend.comercial.dominio.modelo.Filtros;
import com.visual.backend.comercial.dominio.modelo.Proyecto;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;

public interface RepositorioProyectos {

    Optional<Proyecto> buscarPorId(Integer id);

    /** Proyecto vigente o excepcion de recurso no encontrado. */
    default Proyecto obtener(Integer id) {
        return buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("el proyecto", id));
    }

    Optional<Proyecto> buscarPorCotizacion(Integer idCotizacion);

    /** Id del proyecto generado por cada cotizacion aceptada (clave: id de cotizacion). */
    Map<Integer, Integer> idsPorCotizacion(Collection<Integer> idsCotizaciones);

    Pagina<Proyecto> buscar(Filtros.Proyectos filtro, SolicitudPagina solicitud);

    Proyecto guardar(Proyecto proyecto);
}
