package com.visual.backend.logistica.infraestructura.persistencia.adaptador;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import com.visual.backend.comercial.infraestructura.persistencia.entidad.EventoEntidad;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.infraestructura.persistencia.ConsultasJpa;
import com.visual.backend.logistica.dominio.modelo.Filtros;
import com.visual.backend.logistica.dominio.modelo.ResumenLogisticoEvento;
import com.visual.backend.logistica.dominio.modelo.EventoLogistico;
import com.visual.backend.logistica.dominio.puerto.ConsultaEventosLogistica;
import com.visual.backend.logistica.infraestructura.persistencia.jpa.EventoLogisticaJpaRepositorio;
import com.visual.backend.logistica.infraestructura.persistencia.jpa.PlanLogisticoJpaRepositorio;
import com.visual.backend.logistica.infraestructura.persistencia.jpa.PlanLogisticoJpaRepositorio.AvanceLogistico;

import jakarta.persistence.criteria.Predicate;

@Repository
public class ConsultaEventosLogisticaJpa implements ConsultaEventosLogistica {

    private static final Sort ORDEN_POR_DEFECTO = Sort.by("fechaInicio", "id");
    private static final long SIN_AVANCE = 0;

    private final EventoLogisticaJpaRepositorio eventos;
    private final PlanLogisticoJpaRepositorio planes;

    public ConsultaEventosLogisticaJpa(EventoLogisticaJpaRepositorio eventos, PlanLogisticoJpaRepositorio planes) {
        this.eventos = eventos;
        this.planes = planes;
    }

    @Override
    public Optional<EventoLogistico> buscarPorId(Integer idEvento) {
        return eventos.findById(idEvento).filter(evento -> evento.getFechaEliminacion() == null)
                .map(MapeadorLogistica::evento);
    }

    @Override
    public Pagina<ResumenLogisticoEvento> buscar(Filtros.Eventos filtro, SolicitudPagina solicitud) {
        Page<EventoEntidad> pagina = eventos.findAll(especificacion(filtro),
                ConsultasJpa.paginacion(solicitud, ORDEN_POR_DEFECTO));
        List<Integer> ids = pagina.getContent().stream().map(EventoEntidad::getId).toList();
        Map<Integer, AvanceLogistico> avance = ids.isEmpty() ? Map.of()
                : planes.avance(ids).stream().collect(Collectors.toMap(AvanceLogistico::getIdEvento,
                        Function.identity()));
        return ConsultasJpa.pagina(pagina.map(evento -> resumen(evento, avance.get(evento.getId()))));
    }

    private static ResumenLogisticoEvento resumen(EventoEntidad evento, AvanceLogistico avance) {
        return avance == null
                ? new ResumenLogisticoEvento(MapeadorLogistica.evento(evento), SIN_AVANCE, SIN_AVANCE, SIN_AVANCE)
                : new ResumenLogisticoEvento(MapeadorLogistica.evento(evento), avance.getJornadas(),
                        avance.getPersonal(), avance.getCruces());
    }

    /** Eventos vigentes que se cruzan con el rango; el texto busca en evento, proyecto y cliente. */
    private static Specification<EventoEntidad> especificacion(Filtros.Eventos filtro) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.isNull(raiz.get("fechaEliminacion")));
            if (filtro.desde() != null) {
                condiciones.add(cb.greaterThan(raiz.get("fechaFin"), filtro.desde()));
            }
            if (filtro.hasta() != null) {
                condiciones.add(cb.lessThan(raiz.get("fechaInicio"), filtro.hasta()));
            }
            if (filtro.texto() != null && !filtro.texto().isBlank()) {
                String patron = ConsultasJpa.patronContiene(filtro.texto());
                var proyecto = raiz.join("proyecto");
                var cliente = proyecto.join("cotizacion").join("cliente");
                condiciones.add(cb.or(
                        cb.like(cb.lower(raiz.get("nombre")), patron, ConsultasJpa.ESCAPE_LIKE),
                        cb.like(cb.lower(proyecto.get("nombre")), patron, ConsultasJpa.ESCAPE_LIKE),
                        cb.like(cb.lower(cliente.get("razonSocial")), patron, ConsultasJpa.ESCAPE_LIKE)));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
