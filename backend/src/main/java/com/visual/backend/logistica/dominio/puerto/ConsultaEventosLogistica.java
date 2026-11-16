package com.visual.backend.logistica.dominio.puerto;

import java.util.Optional;

import com.visual.backend.compartido.dominio.Pagina;
import com.visual.backend.compartido.dominio.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.logistica.dominio.modelo.EventoLogistico;
import com.visual.backend.logistica.dominio.modelo.Filtros;
import com.visual.backend.logistica.dominio.modelo.ResumenLogisticoEvento;

/** Lectura de los eventos del modulo comercial desde la logistica. */
public interface ConsultaEventosLogistica {

    Optional<EventoLogistico> buscarPorId(Integer idEvento);

    default EventoLogistico obtener(Integer idEvento) {
        return buscarPorId(idEvento).orElseThrow(() -> new RecursoNoEncontradoExcepcion("el evento", idEvento));
    }

    /** El proyecto esta en planificacion o en ejecucion. */
    boolean proyectoVigente(Integer idProyecto);

    Pagina<ResumenLogisticoEvento> buscar(Filtros.Eventos filtro, SolicitudPagina solicitud);
}
