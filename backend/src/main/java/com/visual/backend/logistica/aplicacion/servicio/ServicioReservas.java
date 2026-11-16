package com.visual.backend.logistica.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.compartido.dominio.excepcion.ConflictoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;
import com.visual.backend.logistica.aplicacion.dto.ComandoReserva;
import com.visual.backend.logistica.aplicacion.dto.VistaReserva;
import com.visual.backend.logistica.dominio.modelo.ActivoReservable;
import com.visual.backend.logistica.dominio.modelo.ConflictoReserva;
import com.visual.backend.logistica.dominio.modelo.EventoLogistico;
import com.visual.backend.logistica.dominio.modelo.Filtros;
import com.visual.backend.logistica.dominio.modelo.Jornada;
import com.visual.backend.logistica.dominio.modelo.PeriodoReserva;
import com.visual.backend.logistica.dominio.modelo.PlanLogistico;
import com.visual.backend.logistica.dominio.modelo.ReservaActivo;
import com.visual.backend.logistica.dominio.puerto.ConsultaActivosReservables;
import com.visual.backend.logistica.dominio.puerto.ConsultaEventosLogistica;
import com.visual.backend.logistica.dominio.puerto.RepositorioReservas;

/**
 * Reserva de equipos por evento. El periodo va del primer montaje al ultimo
 * desmontaje y se recalcula cuando cambian las jornadas o el evento. Se
 * reservan todos los equipos pedidos o ninguno.
 */
@Service
public class ServicioReservas {

    public static final String ENTIDAD = "reservas_activo";
    public static final String MOTIVO_EVENTO_CANCELADO = "Evento cancelado";
    static final String MENSAJE_CONFLICTO = "Algunos equipos ya están reservados para otro evento en ese periodo";

    private final ConsultaEventosLogistica eventos;
    private final PlanesEvento planes;
    private final RepositorioReservas reservas;
    private final ConsultaActivosReservables activos;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public ServicioReservas(ConsultaEventosLogistica eventos, PlanesEvento planes, RepositorioReservas reservas,
                            ConsultaActivosReservables activos, ServicioAuditoria auditoria,
                            ContextoSolicitud contexto, Clock reloj) {
        this.eventos = eventos;
        this.planes = planes;
        this.reservas = reservas;
        this.activos = activos;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<VistaReserva> listar(Integer idEvento) {
        eventos.obtener(idEvento);
        return planes.buscar(idEvento).map(plan -> vistas(reservas.buscarActivasPorPlan(plan.getId())))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<ActivoReservable> disponibles(Integer idEvento, Filtros.ActivosReservables filtro) {
        EventoLogistico evento = eventos.obtener(idEvento);
        List<Jornada> jornadas = planes.buscar(idEvento).map(PlanLogistico::getJornadas).orElse(List.of());
        return activos.buscarDisponibles(PeriodoReserva.calcular(evento, jornadas), filtro);
    }

    @Transactional
    public List<VistaReserva> reservar(Integer idEvento, ComandoReserva comando) {
        EventoLogistico evento = eventos.obtener(idEvento);
        exigirReservable(evento);
        Set<Integer> ids = new LinkedHashSet<>(comando.idsActivos());
        Map<Integer, ActivoReservable> encontrados = porId(activos.buscarPorIds(ids));
        PlanLogistico plan = planes.obtenerOCrear(evento);
        PeriodoReserva periodo = PeriodoReserva.calcular(evento, plan.getJornadas());
        exigirNoReservadosEnPlan(plan, encontrados);

        Integer actor = contexto.idUsuarioActual();
        LocalDateTime ahora = ahora();
        List<ReservaActivo> nuevas = new ArrayList<>();
        List<ConflictoReserva> conflictos = new ArrayList<>();
        for (Integer id : ids) {
            ActivoReservable activo = encontrados.get(id);
            if (activo == null) {
                throw new RecursoNoEncontradoExcepcion("el activo", id);
            }
            nuevas.add(ReservaActivo.nueva(plan.getId(), activo, periodo, actor, ahora));
            conflictos.addAll(reservas.buscarSuperpuestas(id, periodo, null));
        }
        exigirSinConflictos(conflictos);
        List<ReservaActivo> guardadas = reservas.guardarTodas(nuevas);
        guardadas.forEach(reserva -> auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD, reserva.getId(),
                null, VistaReserva.mapaAuditoria(reserva), null));
        return vistas(guardadas);
    }

    @Transactional
    public VistaReserva liberar(Integer idEvento, Integer idReserva, String motivo) {
        eventos.obtener(idEvento).exigirEditable();
        PlanLogistico plan = planes.obtenerExistente(idEvento);
        ReservaActivo reserva = reservas.obtener(idReserva);
        if (!reserva.getIdPlan().equals(plan.getId())) {
            throw new RecursoNoEncontradoExcepcion("la reserva", idReserva);
        }
        ReservaActivo liberada = liberarYAuditar(reserva, motivo);
        return vistas(List.of(liberada)).getFirst();
    }

    /**
     * Ajusta el periodo de las reservas activas del plan a sus jornadas y al
     * horario del evento. Si alguna choca con otra reserva no cambia nada.
     */
    @Transactional
    public void recalcular(EventoLogistico evento, PlanLogistico plan) {
        if (plan.getId() == null) {
            return;
        }
        PeriodoReserva periodo = PeriodoReserva.calcular(evento, plan.getJornadas());
        List<ReservaActivo> cambiadas = reservas.buscarActivasPorPlan(plan.getId()).stream()
                .filter(reserva -> !reserva.getPeriodo().equals(periodo)).toList();
        List<ConflictoReserva> conflictos = cambiadas.stream()
                .flatMap(reserva -> reservas.buscarSuperpuestas(reserva.getIdActivo(), periodo, reserva.getId())
                        .stream())
                .toList();
        exigirSinConflictos(conflictos);
        Integer actor = contexto.idUsuarioActual();
        LocalDateTime ahora = ahora();
        for (ReservaActivo reserva : cambiadas) {
            Map<String, Object> anterior = VistaReserva.mapaAuditoria(reserva);
            reserva.reprogramar(periodo, actor, ahora);
            auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, reserva.getId(), anterior,
                    VistaReserva.mapaAuditoria(reserva), "Periodo recalculado");
        }
        reservas.guardarTodas(cambiadas);
    }

    /** El evento cambio de horario: sus reservas siguen al nuevo periodo. */
    @Transactional
    public void recalcularEvento(Integer idEvento) {
        planes.buscar(idEvento).ifPresent(plan -> recalcular(eventos.obtener(idEvento), plan));
    }

    @Transactional
    public void liberarPorCancelacion(Integer idEvento) {
        planes.buscar(idEvento).ifPresent(plan -> reservas.buscarActivasPorPlan(plan.getId())
                .forEach(reserva -> liberarYAuditar(reserva, MOTIVO_EVENTO_CANCELADO)));
    }

    private ReservaActivo liberarYAuditar(ReservaActivo reserva, String motivo) {
        Map<String, Object> anterior = VistaReserva.mapaAuditoria(reserva);
        reserva.liberar(motivo, contexto.idUsuarioActual(), ahora());
        ReservaActivo guardada = reservas.guardar(reserva);
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, guardada.getId(), anterior,
                VistaReserva.mapaAuditoria(guardada), "Liberación de la reserva");
        return guardada;
    }

    private void exigirReservable(EventoLogistico evento) {
        evento.exigirEditable();
        if (!eventos.proyectoVigente(evento.idProyecto())) {
            throw new ReglaNegocioExcepcion("Solo se reservan equipos para proyectos en planificación o en ejecución");
        }
    }

    private void exigirNoReservadosEnPlan(PlanLogistico plan, Map<Integer, ActivoReservable> solicitados) {
        String repetidos = reservas.buscarActivasPorPlan(plan.getId()).stream()
                .map(ReservaActivo::getIdActivo).filter(solicitados::containsKey)
                .map(id -> solicitados.get(id).codigo()).collect(Collectors.joining(", "));
        if (!repetidos.isEmpty()) {
            throw new ReglaNegocioExcepcion("Ya están reservados para este evento: " + repetidos);
        }
    }

    private static void exigirSinConflictos(List<ConflictoReserva> conflictos) {
        if (!conflictos.isEmpty()) {
            throw new ConflictoExcepcion(MENSAJE_CONFLICTO, conflictos);
        }
    }

    private List<VistaReserva> vistas(List<ReservaActivo> lista) {
        Map<Integer, ActivoReservable> datos = porId(activos.buscarPorIds(
                lista.stream().map(ReservaActivo::getIdActivo).collect(Collectors.toSet())));
        return lista.stream().map(reserva -> VistaReserva.de(reserva, datos.get(reserva.getIdActivo()))).toList();
    }

    private static Map<Integer, ActivoReservable> porId(List<ActivoReservable> lista) {
        return lista.stream().collect(Collectors.toMap(ActivoReservable::id, Function.identity()));
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }
}
