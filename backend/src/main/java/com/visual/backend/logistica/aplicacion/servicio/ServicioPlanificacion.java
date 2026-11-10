package com.visual.backend.logistica.aplicacion.servicio;

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
import com.visual.backend.comercial.dominio.modelo.ElementoCatalogo;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;
import com.visual.backend.logistica.aplicacion.dto.ComandoAsignacion;
import com.visual.backend.logistica.aplicacion.dto.ComandoJornada;
import com.visual.backend.logistica.aplicacion.dto.VistaAsignacion;
import com.visual.backend.logistica.aplicacion.dto.VistaJornada;
import com.visual.backend.logistica.aplicacion.dto.VistaPlanEvento;
import com.visual.backend.logistica.dominio.modelo.Asignacion;
import com.visual.backend.logistica.dominio.modelo.EventoLogistico;
import com.visual.backend.logistica.dominio.modelo.Filtros;
import com.visual.backend.logistica.dominio.modelo.Jornada;
import com.visual.backend.logistica.dominio.modelo.Periodo;
import com.visual.backend.logistica.dominio.modelo.PersonalTecnico;
import com.visual.backend.logistica.dominio.modelo.PlanLogistico;
import com.visual.backend.logistica.dominio.modelo.ResumenLogisticoEvento;
import com.visual.backend.logistica.dominio.puerto.ConsultaEventosLogistica;
import com.visual.backend.logistica.dominio.puerto.RepositorioAsignaciones;
import com.visual.backend.logistica.dominio.puerto.RepositorioCatalogosLogistica;
import com.visual.backend.logistica.dominio.puerto.RepositorioPersonal;
import com.visual.backend.logistica.dominio.puerto.RepositorioPlanes;

/**
 * Planificacion logistica de los eventos: transporte, jornadas de montaje y
 * desmontaje y asignacion de personal. El plan se crea con su primer cambio.
 */
@Service
public class ServicioPlanificacion {

    public static final String ENTIDAD_PLAN = "planes_logisticos";
    public static final String ENTIDAD_ASIGNACION = "asignaciones_personal";

    private final ConsultaEventosLogistica eventos;
    private final RepositorioPlanes planes;
    private final RepositorioAsignaciones asignaciones;
    private final RepositorioPersonal personal;
    private final RepositorioCatalogosLogistica catalogos;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public ServicioPlanificacion(ConsultaEventosLogistica eventos, RepositorioPlanes planes,
                                 RepositorioAsignaciones asignaciones, RepositorioPersonal personal,
                                 RepositorioCatalogosLogistica catalogos, ServicioAuditoria auditoria,
                                 ContextoSolicitud contexto, Clock reloj) {
        this.eventos = eventos;
        this.planes = planes;
        this.asignaciones = asignaciones;
        this.personal = personal;
        this.catalogos = catalogos;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Pagina<ResumenLogisticoEvento> listarEventos(Filtros.Eventos filtro, SolicitudPagina solicitud) {
        return eventos.buscar(filtro, solicitud);
    }

    @Transactional(readOnly = true)
    public VistaPlanEvento detalle(Integer idEvento) {
        EventoLogistico evento = eventos.obtener(idEvento);
        return planes.buscarPorEvento(idEvento).map(plan -> vista(evento, plan))
                .orElseGet(() -> new VistaPlanEvento(evento, evento.editable(), null, evento.fechaInicio(),
                        evento.fechaFin(), List.of(), List.of(), List.of()));
    }

    @Transactional
    public VistaPlanEvento guardarTransporte(Integer idEvento, String detalles) {
        EventoLogistico evento = eventos.obtener(idEvento);
        PlanLogistico plan = planOCrear(evento);
        Map<String, Object> anterior = mapa(plan);
        plan.actualizarTransporte(evento, detalles, contexto.idUsuarioActual(), ahora());
        return guardarPlan(evento, plan, anterior);
    }

    @Transactional
    public VistaPlanEvento agregarJornada(Integer idEvento, ComandoJornada comando) {
        EventoLogistico evento = eventos.obtener(idEvento);
        PlanLogistico plan = planOCrear(evento);
        Map<String, Object> anterior = mapa(plan);
        plan.agregarJornada(evento, jornada(null, comando), contexto.idUsuarioActual(), ahora());
        return guardarPlan(evento, plan, anterior);
    }

    @Transactional
    public VistaPlanEvento modificarJornada(Integer idEvento, Integer idJornada, ComandoJornada comando) {
        EventoLogistico evento = eventos.obtener(idEvento);
        PlanLogistico plan = planExistente(idEvento);
        Map<String, Object> anterior = mapa(plan);
        plan.modificarJornada(evento, jornada(idJornada, comando), contexto.idUsuarioActual(), ahora());
        exigirAsignacionesDentro(evento, plan);
        return guardarPlan(evento, plan, anterior);
    }

    @Transactional
    public VistaPlanEvento quitarJornada(Integer idEvento, Integer idJornada) {
        EventoLogistico evento = eventos.obtener(idEvento);
        PlanLogistico plan = planExistente(idEvento);
        Map<String, Object> anterior = mapa(plan);
        plan.quitarJornada(evento, idJornada, contexto.idUsuarioActual(), ahora());
        exigirAsignacionesDentro(evento, plan);
        return guardarPlan(evento, plan, anterior);
    }

    @Transactional
    public VistaAsignacion asignar(Integer idEvento, ComandoAsignacion comando) {
        EventoLogistico evento = eventos.obtener(idEvento);
        PlanLogistico plan = planOCrear(evento);
        PersonalTecnico persona = personal.obtener(comando.idPersonal());
        Periodo periodo = new Periodo(comando.inicio(), comando.fin());
        exigirSinDuplicado(plan, persona, periodo, null);
        Asignacion asignacion = asignaciones.guardar(Asignacion.nueva(plan, evento, persona, rol(comando), periodo,
                contexto.idUsuarioActual(), ahora()));
        auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD_ASIGNACION, asignacion.getId(), null,
                VistaAsignacion.mapaAuditoria(asignacion), null);
        return conCruces(asignacion);
    }

    @Transactional
    public VistaAsignacion modificarAsignacion(Integer idEvento, Integer idAsignacion, ComandoAsignacion comando) {
        EventoLogistico evento = eventos.obtener(idEvento);
        PlanLogistico plan = planExistente(idEvento);
        Asignacion asignacion = asignacionDelPlan(plan, idAsignacion);
        Map<String, Object> anterior = VistaAsignacion.mapaAuditoria(asignacion);
        PersonalTecnico persona = personal.obtener(comando.idPersonal());
        Periodo periodo = new Periodo(comando.inicio(), comando.fin());
        exigirSinDuplicado(plan, persona, periodo, idAsignacion);
        asignacion.actualizar(plan, evento, persona, rol(comando), periodo, contexto.idUsuarioActual(), ahora());
        Asignacion guardada = asignaciones.guardar(asignacion);
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD_ASIGNACION, idAsignacion, anterior,
                VistaAsignacion.mapaAuditoria(guardada), null);
        return conCruces(guardada);
    }

    @Transactional
    public void quitarAsignacion(Integer idEvento, Integer idAsignacion) {
        eventos.obtener(idEvento).exigirEditable();
        Asignacion asignacion = asignacionDelPlan(planExistente(idEvento), idAsignacion);
        asignaciones.eliminar(idAsignacion);
        auditoria.registrarOperacion(AccionAuditoria.ELIMINAR, ENTIDAD_ASIGNACION, idAsignacion,
                VistaAsignacion.mapaAuditoria(asignacion), null, null);
    }

    private VistaPlanEvento guardarPlan(EventoLogistico evento, PlanLogistico plan, Map<String, Object> anterior) {
        PlanLogistico guardado = planes.guardar(plan);
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD_PLAN, guardado.getId(), anterior,
                mapa(guardado), null);
        return vista(evento, guardado);
    }

    private VistaPlanEvento vista(EventoLogistico evento, PlanLogistico plan) {
        Periodo ventana = plan.ventana(evento);
        List<VistaAsignacion> asignadas = plan.getId() == null ? List.of()
                : asignaciones.listarPorPlan(plan.getId()).stream().map(this::conCruces).toList();
        return new VistaPlanEvento(evento, evento.editable(), plan.getDetallesTransporte(), ventana.inicio(),
                ventana.fin(), plan.getJornadas().stream().map(VistaJornada::de).toList(), asignadas,
                plan.avisos(evento));
    }

    private VistaAsignacion conCruces(Asignacion asignacion) {
        return VistaAsignacion.de(asignacion, asignaciones.cruces(asignacion.getPersonal().id(),
                asignacion.getPeriodo(), asignacion.getIdPlan()));
    }

    /** Un plan nuevo se guarda primero para que sus jornadas y asignaciones tengan a quien pertenecer. */
    private PlanLogistico planOCrear(EventoLogistico evento) {
        return planes.buscarPorEvento(evento.id()).orElseGet(() -> {
            PlanLogistico creado = planes.guardar(PlanLogistico.nuevo(evento, contexto.idUsuarioActual(), ahora()));
            auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD_PLAN, creado.getId(), null, mapa(creado),
                    null);
            return creado;
        });
    }

    private PlanLogistico planExistente(Integer idEvento) {
        return planes.buscarPorEvento(idEvento)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el plan logístico del evento", idEvento));
    }

    private Asignacion asignacionDelPlan(PlanLogistico plan, Integer idAsignacion) {
        Asignacion asignacion = asignaciones.obtener(idAsignacion);
        if (!asignacion.getIdPlan().equals(plan.getId())) {
            throw new RecursoNoEncontradoExcepcion("la asignación", idAsignacion);
        }
        return asignacion;
    }

    private void exigirAsignacionesDentro(EventoLogistico evento, PlanLogistico plan) {
        if (asignaciones.existenFueraDe(plan.getId(), plan.ventana(evento))) {
            throw new ReglaNegocioExcepcion("El cambio deja asignaciones de personal fuera del plan; ajústelas primero");
        }
    }

    private void exigirSinDuplicado(PlanLogistico plan, PersonalTecnico persona, Periodo periodo,
                                    Integer idExcluido) {
        if (plan.getId() != null && asignaciones.existeEnPlan(plan.getId(), persona.getId(), periodo, idExcluido)) {
            throw new ReglaNegocioExcepcion(persona.nombreCompleto()
                    + " ya tiene una asignación en ese horario en este evento");
        }
    }

    private ElementoCatalogo rol(ComandoAsignacion comando) {
        return catalogos.buscarRolEvento(comando.idRol())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el rol", comando.idRol()));
    }

    private static Jornada jornada(Integer id, ComandoJornada comando) {
        return new Jornada(id, comando.tipo(), new Periodo(comando.inicio(), comando.fin()), comando.descripcion());
    }

    private static Map<String, Object> mapa(PlanLogistico plan) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("evento", plan.getIdEvento());
        mapa.put("detallesTransporte", plan.getDetallesTransporte());
        mapa.put("jornadas", plan.getJornadas().stream()
                .map(jornada -> jornada.tipo() + " " + jornada.periodo().inicio() + " - " + jornada.periodo().fin())
                .toList());
        return mapa;
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }
}
