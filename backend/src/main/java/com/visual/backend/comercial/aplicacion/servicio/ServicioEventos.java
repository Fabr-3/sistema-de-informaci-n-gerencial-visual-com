package com.visual.backend.comercial.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.comercial.aplicacion.dto.ComandoEvento;
import com.visual.backend.comercial.dominio.modelo.DatosEvento;
import com.visual.backend.comercial.dominio.modelo.EstadoEvento;
import com.visual.backend.comercial.dominio.modelo.Evento;
import com.visual.backend.comercial.dominio.modelo.EventoCalendario;
import com.visual.backend.comercial.dominio.modelo.Filtros;
import com.visual.backend.comercial.dominio.modelo.Proyecto;
import com.visual.backend.comercial.dominio.puerto.RepositorioCatalogosComerciales;
import com.visual.backend.comercial.dominio.puerto.RepositorioEventos;
import com.visual.backend.comercial.dominio.puerto.RepositorioProyectos;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/** Cronograma de eventos de los proyectos. */
@Service
public class ServicioEventos {

    public static final String ENTIDAD = "eventos";
    /** Rango maximo consultable en el calendario, para acotar la consulta. */
    private static final long DIAS_MAXIMOS_CALENDARIO = 400;

    private final RepositorioEventos repositorio;
    private final RepositorioProyectos proyectos;
    private final RepositorioCatalogosComerciales catalogos;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public ServicioEventos(RepositorioEventos repositorio, RepositorioProyectos proyectos,
                           RepositorioCatalogosComerciales catalogos, ServicioAuditoria auditoria,
                           ContextoSolicitud contexto, Clock reloj) {
        this.repositorio = repositorio;
        this.proyectos = proyectos;
        this.catalogos = catalogos;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<EventoCalendario> calendario(Filtros.Eventos filtro) {
        if (!filtro.hasta().isAfter(filtro.desde())
                || filtro.desde().plusDays(DIAS_MAXIMOS_CALENDARIO).isBefore(filtro.hasta())) {
            throw new ReglaNegocioExcepcion("Rango de fechas inválido para el cronograma");
        }
        return repositorio.buscarCalendario(filtro);
    }

    @Transactional
    public Evento crear(Integer idProyecto, ComandoEvento comando) {
        Proyecto proyecto = proyectos.obtener(idProyecto);
        Evento evento = repositorio.guardar(Evento.nuevo(proyecto, datos(comando), contexto.idUsuarioActual(),
                LocalDateTime.now(reloj)));
        auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD, evento.getId(), null, mapa(evento), null);
        return evento;
    }

    @Transactional
    public Evento actualizar(Integer idProyecto, Integer idEvento, ComandoEvento comando) {
        Proyecto proyecto = proyectos.obtener(idProyecto);
        Evento evento = buscarDelProyecto(idProyecto, idEvento);
        Map<String, Object> anterior = mapa(evento);
        evento.actualizar(proyecto, datos(comando), contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        Evento guardado = repositorio.guardar(evento);
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, idEvento, anterior, mapa(guardado), null);
        return guardado;
    }

    /** Un evento programado se marca ejecutado o se cancela. */
    @Transactional
    public Evento cambiarEstado(Integer idProyecto, Integer idEvento, EstadoEvento estado) {
        Evento evento = buscarDelProyecto(idProyecto, idEvento);
        Map<String, Object> anterior = mapa(evento);
        Integer actor = contexto.idUsuarioActual();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        switch (estado) {
            case EJECUTADO -> evento.marcarEjecutado(actor, ahora);
            case CANCELADO -> evento.cancelar(actor, ahora);
            default -> throw new ReglaNegocioExcepcion("Un evento no puede volver a programado");
        }
        Evento guardado = repositorio.guardar(evento);
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, idEvento, anterior, mapa(guardado),
                "Cambio de estado a " + estado);
        return guardado;
    }

    private Evento buscarDelProyecto(Integer idProyecto, Integer idEvento) {
        Evento evento = repositorio.obtener(idEvento);
        if (!evento.getIdProyecto().equals(idProyecto)) {
            throw new RecursoNoEncontradoExcepcion("el evento", idEvento);
        }
        return evento;
    }

    private DatosEvento datos(ComandoEvento comando) {
        return new DatosEvento(catalogos.buscarLugar(comando.idLugar())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el lugar", comando.idLugar())),
                comando.nombre(), comando.fechaInicio(), comando.fechaFin());
    }

    private static Map<String, Object> mapa(Evento evento) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("nombre", evento.getNombre());
        mapa.put("proyecto", evento.getIdProyecto());
        mapa.put("lugar", evento.getLugar().nombre());
        mapa.put("fechaInicio", evento.getFechaInicio().toString());
        mapa.put("fechaFin", evento.getFechaFin().toString());
        mapa.put("estado", evento.getEstado().name());
        return mapa;
    }
}
