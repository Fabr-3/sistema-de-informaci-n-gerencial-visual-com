package com.visual.backend.comercial.infraestructura.persistencia.adaptador;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.visual.backend.comercial.dominio.modelo.EstadoEvento;
import com.visual.backend.comercial.dominio.modelo.Evento;
import com.visual.backend.comercial.dominio.modelo.EventoCalendario;
import com.visual.backend.comercial.dominio.modelo.Filtros;
import com.visual.backend.comercial.dominio.puerto.RepositorioEventos;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.EventoEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.jpa.EventoJpaRepositorio;
import com.visual.backend.comercial.infraestructura.persistencia.jpa.LugarJpaRepositorio;
import com.visual.backend.comercial.infraestructura.persistencia.jpa.ProyectoJpaRepositorio;

@Repository
public class RepositorioEventosJpa implements RepositorioEventos {

    /** Limites usados cuando el periodo del proyecto no tiene fecha de inicio o de fin. */
    private static final LocalDate SIN_LIMITE_INFERIOR = LocalDate.of(1900, 1, 1);
    private static final LocalDate SIN_LIMITE_SUPERIOR = LocalDate.of(9999, 12, 31);

    private final EventoJpaRepositorio eventos;
    private final ProyectoJpaRepositorio proyectos;
    private final LugarJpaRepositorio lugares;

    public RepositorioEventosJpa(EventoJpaRepositorio eventos, ProyectoJpaRepositorio proyectos,
                                 LugarJpaRepositorio lugares) {
        this.eventos = eventos;
        this.proyectos = proyectos;
        this.lugares = lugares;
    }

    @Override
    public Optional<Evento> buscarPorId(Integer id) {
        return eventos.findByIdAndFechaEliminacionIsNull(id).map(MapeadorComercial::aDominio);
    }

    @Override
    public List<Evento> listarPorProyecto(Integer idProyecto) {
        return eventos.findByProyectoIdAndFechaEliminacionIsNullOrderByFechaInicioAsc(idProyecto).stream()
                .map(MapeadorComercial::aDominio).toList();
    }

    @Override
    public List<EventoCalendario> buscarCalendario(Filtros.Eventos filtro) {
        return eventos.buscarEnRango(filtro.desde(), filtro.hasta(), filtro.idProyecto()).stream()
                .map(MapeadorComercial::calendario).toList();
    }

    @Override
    public boolean existenFueraDePeriodo(Integer idProyecto, LocalDate inicio, LocalDate fin) {
        return eventos.existenFueraDe(idProyecto, inicio == null ? SIN_LIMITE_INFERIOR : inicio,
                fin == null ? SIN_LIMITE_SUPERIOR : fin, EstadoEvento.CANCELADO);
    }

    @Override
    public Evento guardar(Evento evento) {
        EventoEntidad entidad = evento.getId() == null ? new EventoEntidad() : eventos.getReferenceById(evento.getId());
        if (evento.getId() == null) {
            entidad.setProyecto(proyectos.getReferenceById(evento.getIdProyecto()));
        }
        entidad.setLugar(lugares.getReferenceById(evento.getLugar().id()));
        entidad.setNombre(evento.getNombre());
        entidad.setFechaInicio(evento.getFechaInicio());
        entidad.setFechaFin(evento.getFechaFin());
        entidad.setEstado(evento.getEstado());
        entidad.setCreadoPor(evento.getCreadoPor());
        entidad.setFechaCreacion(evento.getFechaCreacion());
        entidad.setModificadoPor(evento.getModificadoPor());
        entidad.setFechaModificacion(evento.getFechaModificacion());
        return MapeadorComercial.aDominio(eventos.save(entidad));
    }
}
