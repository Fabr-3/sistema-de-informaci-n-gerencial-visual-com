package com.visual.backend.comercial.dominio.puerto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;

import com.visual.backend.comercial.dominio.modelo.Evento;
import com.visual.backend.comercial.dominio.modelo.EventoCalendario;
import com.visual.backend.comercial.dominio.modelo.Filtros;

public interface RepositorioEventos {

    Optional<Evento> buscarPorId(Integer id);

    /** Evento vigente o excepcion de recurso no encontrado. */
    default Evento obtener(Integer id) {
        return buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("el evento", id));
    }

    List<Evento> listarPorProyecto(Integer idProyecto);

    List<EventoCalendario> buscarCalendario(Filtros.Eventos filtro);

    /**
     * Indica si el proyecto tiene eventos no cancelados fuera del periodo
     * indicado (nulo = sin limite por ese lado).
     */
    boolean existenFueraDePeriodo(Integer idProyecto, LocalDate inicio, LocalDate fin);

    Evento guardar(Evento evento);
}
