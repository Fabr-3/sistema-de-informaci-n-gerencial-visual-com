package com.visual.backend.finanzas.dominio.puerto;

import java.util.Optional;

import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.finanzas.dominio.modelo.DetalleResultadoProyecto;
import com.visual.backend.finanzas.dominio.modelo.Filtros;
import com.visual.backend.finanzas.dominio.modelo.ResultadoProyecto;

/** Agregados de ingresos y costos (solo movimientos registrados) por proyecto y evento. */
public interface ConsultaResultados {

    Pagina<ResultadoProyecto> buscarProyectos(Filtros.ResultadosProyectos filtro, SolicitudPagina solicitud);

    Optional<DetalleResultadoProyecto> detalleProyecto(Integer idProyecto);

    default DetalleResultadoProyecto obtenerDetalle(Integer idProyecto) {
        return detalleProyecto(idProyecto).orElseThrow(() -> new RecursoNoEncontradoExcepcion("el proyecto",
                idProyecto));
    }
}
