package com.visual.backend.logistica.dominio.modelo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Plan logistico de un evento: jornadas de montaje (antes del evento) y de
 * desmontaje (despues), y los detalles de transporte. Su ventana, desde el
 * primer montaje hasta el ultimo desmontaje, limita las asignaciones.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlanLogistico {

    /** Evita fechas mal escritas: una jornada no se aleja mas de esto del evento. */
    static final int DIAS_MAXIMOS_DESDE_EVENTO = 30;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Integer id;
    private Integer idEvento;
    private String detallesTransporte;
    private List<Jornada> jornadas;
    private Integer creadoPor;
    private LocalDateTime fechaCreacion;
    private Integer modificadoPor;
    private LocalDateTime fechaModificacion;

    public static PlanLogistico nuevo(EventoLogistico evento, Integer actor, LocalDateTime ahora) {
        evento.exigirEditable();
        return PlanLogistico.builder()
                .idEvento(evento.id())
                .jornadas(List.of())
                .creadoPor(actor)
                .fechaCreacion(ahora)
                .build();
    }

    public void actualizarTransporte(EventoLogistico evento, String detalles, Integer actor, LocalDateTime ahora) {
        evento.exigirEditable();
        detallesTransporte = detalles == null || detalles.isBlank() ? null : detalles.trim();
        registrarModificacion(actor, ahora);
    }

    public void agregarJornada(EventoLogistico evento, Jornada jornada, Integer actor, LocalDateTime ahora) {
        evento.exigirEditable();
        validarJornada(evento, jornada, jornadas);
        List<Jornada> resultado = new ArrayList<>(jornadas);
        resultado.add(jornada.conId(null));
        ordenarYAsignar(resultado, actor, ahora);
    }

    public void modificarJornada(EventoLogistico evento, Jornada jornada, Integer actor, LocalDateTime ahora) {
        evento.exigirEditable();
        jornadaPorId(jornada.id());
        List<Jornada> otras = jornadas.stream().filter(existente -> !jornada.id().equals(existente.id())).toList();
        validarJornada(evento, jornada, otras);
        List<Jornada> resultado = new ArrayList<>(otras);
        resultado.add(jornada);
        ordenarYAsignar(resultado, actor, ahora);
    }

    public void quitarJornada(EventoLogistico evento, Integer idJornada, Integer actor, LocalDateTime ahora) {
        evento.exigirEditable();
        jornadaPorId(idJornada);
        ordenarYAsignar(jornadas.stream().filter(existente -> !idJornada.equals(existente.id())).toList(),
                actor, ahora);
    }

    public Jornada jornadaPorId(Integer idJornada) {
        return jornadas.stream().filter(jornada -> idJornada.equals(jornada.id())).findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("la jornada", idJornada));
    }

    /** Desde el primer montaje (o el inicio del evento) hasta el ultimo desmontaje (o su fin). */
    public Periodo ventana(EventoLogistico evento) {
        return jornadas.stream().map(Jornada::periodo).reduce(evento.periodo(), Periodo::abarcar);
    }

    /** Jornadas que quedaron fuera de orden porque el evento cambio de horario. */
    public List<String> avisos(EventoLogistico evento) {
        List<String> avisos = new ArrayList<>();
        for (Jornada jornada : jornadas) {
            String problema = problemaConEvento(evento, jornada);
            if (problema != null) {
                avisos.add(problema);
            }
        }
        return avisos;
    }

    private static void validarJornada(EventoLogistico evento, Jornada jornada, List<Jornada> otras) {
        String problema = problemaConEvento(evento, jornada);
        if (problema != null) {
            throw new ReglaNegocioExcepcion(problema);
        }
        if (otras.stream().anyMatch(otra -> otra.periodo().seCruzaCon(jornada.periodo()))) {
            throw new ReglaNegocioExcepcion("La jornada se cruza con otra jornada del plan");
        }
    }

    private static String problemaConEvento(EventoLogistico evento, Jornada jornada) {
        Periodo periodo = jornada.periodo();
        if (jornada.tipo() == TipoJornada.MONTAJE) {
            if (periodo.fin().isAfter(evento.fechaInicio())) {
                return "El montaje debe terminar antes del inicio del evento (" + formato(evento.fechaInicio()) + ")";
            }
            if (periodo.inicio().isBefore(evento.fechaInicio().minusDays(DIAS_MAXIMOS_DESDE_EVENTO))) {
                return "El montaje no puede comenzar más de " + DIAS_MAXIMOS_DESDE_EVENTO + " días antes del evento";
            }
        } else {
            if (periodo.inicio().isBefore(evento.fechaFin())) {
                return "El desmontaje debe comenzar después del fin del evento (" + formato(evento.fechaFin()) + ")";
            }
            if (periodo.fin().isAfter(evento.fechaFin().plusDays(DIAS_MAXIMOS_DESDE_EVENTO))) {
                return "El desmontaje no puede terminar más de " + DIAS_MAXIMOS_DESDE_EVENTO
                        + " días después del evento";
            }
        }
        return null;
    }

    private void ordenarYAsignar(List<Jornada> nuevas, Integer actor, LocalDateTime ahora) {
        jornadas = nuevas.stream().sorted(Comparator.comparing(jornada -> jornada.periodo().inicio())).toList();
        registrarModificacion(actor, ahora);
    }

    private void registrarModificacion(Integer actor, LocalDateTime ahora) {
        if (id != null) {
            modificadoPor = actor;
            fechaModificacion = ahora;
        }
    }

    static String formato(LocalDateTime fecha) {
        return fecha.format(FORMATO_FECHA);
    }
}
