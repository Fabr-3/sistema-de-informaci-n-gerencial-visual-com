package com.visual.backend.subcontratacion.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.compartido.dominio.Pagina;
import com.visual.backend.compartido.dominio.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;
import com.visual.backend.subcontratacion.aplicacion.dto.ComandoConsumo;
import com.visual.backend.subcontratacion.aplicacion.dto.VistaConsumo;
import com.visual.backend.subcontratacion.dominio.modelo.Consumible;
import com.visual.backend.subcontratacion.dominio.modelo.ConsumoProyecto;
import com.visual.backend.subcontratacion.dominio.modelo.EventoAsignado;
import com.visual.backend.subcontratacion.dominio.modelo.Filtros;
import com.visual.backend.subcontratacion.dominio.modelo.MovimientoConsumible;
import com.visual.backend.subcontratacion.dominio.puerto.AutorizacionConsumos;
import com.visual.backend.subcontratacion.dominio.puerto.ConsultaAsignacionesTecnico;
import com.visual.backend.subcontratacion.dominio.puerto.RepositorioConsumibles;
import com.visual.backend.subcontratacion.dominio.puerto.RepositorioConsumos;

/**
 * Consumo de consumibles en proyectos y eventos. Descuenta el stock en la
 * misma transaccion; anularlo lo devuelve. El tecnico solo registra y ve
 * consumos de los eventos donde esta asignado.
 */
@Service
public class ServicioConsumos {

    public static final String ENTIDAD = "consumos_proyecto";
    /** El tecnico registra consumos de eventos cuya asignacion termino hace a lo sumo estos dias. */
    static final int DIAS_EVENTOS_RECIENTES = 90;
    static final String MENSAJE_NO_ASIGNADO = "Solo puede registrar consumos de los eventos donde está asignado";

    private final RepositorioConsumos repositorio;
    private final RepositorioConsumibles consumibles;
    private final ReferenciasImputacion referencias;
    private final ConsultaAsignacionesTecnico asignaciones;
    private final AutorizacionConsumos autorizacion;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public ServicioConsumos(RepositorioConsumos repositorio, RepositorioConsumibles consumibles,
                            ReferenciasImputacion referencias, ConsultaAsignacionesTecnico asignaciones,
                            AutorizacionConsumos autorizacion, ServicioAuditoria auditoria,
                            ContextoSolicitud contexto, Clock reloj) {
        this.repositorio = repositorio;
        this.consumibles = consumibles;
        this.referencias = referencias;
        this.asignaciones = asignaciones;
        this.autorizacion = autorizacion;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Pagina<VistaConsumo> listar(Filtros.Consumos filtro, SolicitudPagina solicitud) {
        Filtros.Consumos alcance = autorizacion.limitadoAEventosAsignados()
                ? filtro.soloEventos(eventosAsignados().stream().map(EventoAsignado::idEvento).toList())
                : filtro;
        return repositorio.buscar(alcance, solicitud).mapear(VistaConsumo::de);
    }

    /** Eventos recientes o proximos donde trabaja el usuario (para el formulario del tecnico). */
    @Transactional(readOnly = true)
    public List<EventoAsignado> eventosAsignados() {
        return asignaciones.eventosAsignados(ahora().minusDays(DIAS_EVENTOS_RECIENTES));
    }

    @Transactional
    public VistaConsumo registrar(ComandoConsumo comando) {
        exigirAlcance(comando.idEvento());
        ElementoCatalogo proyecto = referencias.proyectoVigente(comando.idProyecto());
        ElementoCatalogo evento = referencias.evento(comando.idEvento(), proyecto.id());
        Consumible consumible = consumibles.obtenerParaActualizar(comando.idConsumible());
        Integer actor = contexto.idUsuarioActual();
        LocalDateTime ahora = ahora();
        ConsumoProyecto consumo = ConsumoProyecto.registrar(consumible, proyecto, evento,
                comando.cantidad(), comando.fecha(), actor, ahora);
        MovimientoConsumible movimiento = consumible.consumir(comando.cantidad(), consumo.referencia(), actor,
                ahora);
        ConsumoProyecto guardado = repositorio.guardar(consumo);
        consumibles.guardar(consumible);
        consumibles.registrarMovimiento(movimiento);
        auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD, guardado.getId(), null,
                VistaConsumo.mapaAuditoria(guardado), null);
        return VistaConsumo.de(guardado);
    }

    @Transactional
    public VistaConsumo anular(Integer id, String motivo) {
        ConsumoProyecto consumo = repositorio.obtener(id);
        exigirAlcance(consumo.getEvento() == null ? null : consumo.getEvento().id());
        Map<String, Object> anterior = VistaConsumo.mapaAuditoria(consumo);
        Integer actor = contexto.idUsuarioActual();
        LocalDateTime ahora = ahora();
        consumo.anular(motivo, actor, ahora);
        Consumible consumible = consumibles.obtenerParaActualizar(consumo.getConsumible().id());
        consumibles.registrarMovimiento(consumible.devolver(consumo.getCantidad(), "Anulación: "
                + consumo.referencia(), actor, ahora));
        consumibles.guardar(consumible);
        ConsumoProyecto anulado = repositorio.guardar(consumo);
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id, anterior,
                VistaConsumo.mapaAuditoria(anulado), "Anulación del consumo");
        return VistaConsumo.de(anulado);
    }

    /** El tecnico solo opera sobre eventos donde esta asignado (no sobre el proyecto en general). */
    private void exigirAlcance(Integer idEvento) {
        if (!autorizacion.limitadoAEventosAsignados()) {
            return;
        }
        if (idEvento == null) {
            throw new ReglaNegocioExcepcion("Indique el evento donde se usó el consumible");
        }
        boolean asignado = eventosAsignados().stream().anyMatch(evento -> evento.idEvento().equals(idEvento));
        if (!asignado) {
            throw new AccessDeniedException(MENSAJE_NO_ASIGNADO);
        }
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }
}
