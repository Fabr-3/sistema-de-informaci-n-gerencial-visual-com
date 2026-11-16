package com.visual.backend.logistica.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.logistica.dominio.modelo.EventoLogistico;
import com.visual.backend.logistica.dominio.modelo.PlanLogistico;
import com.visual.backend.logistica.dominio.puerto.RepositorioPlanes;

/** Obtencion del plan logistico de un evento, comun a la planificacion y a las reservas. */
@Component
public class PlanesEvento {

    public static final String ENTIDAD_PLAN = "planes_logisticos";

    private final RepositorioPlanes planes;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public PlanesEvento(RepositorioPlanes planes, ServicioAuditoria auditoria, ContextoSolicitud contexto,
                        Clock reloj) {
        this.planes = planes;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.reloj = reloj;
    }

    public Optional<PlanLogistico> buscar(Integer idEvento) {
        return planes.buscarPorEvento(idEvento);
    }

    /** Un plan nuevo se guarda primero para que sus jornadas, asignaciones y reservas tengan a quien pertenecer. */
    public PlanLogistico obtenerOCrear(EventoLogistico evento) {
        return planes.buscarPorEvento(evento.id()).orElseGet(() -> {
            PlanLogistico creado = planes.guardar(PlanLogistico.nuevo(evento, contexto.idUsuarioActual(),
                    LocalDateTime.now(reloj)));
            auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD_PLAN, creado.getId(), null, mapa(creado),
                    null);
            return creado;
        });
    }

    public PlanLogistico obtenerExistente(Integer idEvento) {
        return planes.buscarPorEvento(idEvento)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el plan logístico del evento", idEvento));
    }

    public PlanLogistico guardar(PlanLogistico plan) {
        return planes.guardar(plan);
    }

    static Map<String, Object> mapa(PlanLogistico plan) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("evento", plan.getIdEvento());
        mapa.put("detallesTransporte", plan.getDetallesTransporte());
        mapa.put("jornadas", plan.getJornadas().stream()
                .map(jornada -> jornada.tipo() + " " + jornada.periodo().inicio() + " - " + jornada.periodo().fin())
                .toList());
        return mapa;
    }
}
