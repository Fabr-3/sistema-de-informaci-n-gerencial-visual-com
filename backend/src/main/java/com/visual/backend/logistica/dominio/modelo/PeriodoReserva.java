package com.visual.backend.logistica.dominio.modelo;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/** Intervalo [salida, retorno) en que un activo queda comprometido con un evento. */
public record PeriodoReserva(LocalDateTime salida, LocalDateTime retorno) {

    public PeriodoReserva {
        if (salida == null || retorno == null || !retorno.isAfter(salida)) {
            throw new ReglaNegocioExcepcion("El retorno de la reserva debe ser posterior a la salida");
        }
    }

    /**
     * Desde el montaje mas temprano hasta el desmontaje mas tardio; sin
     * montaje o sin desmontaje se usa el inicio o el fin del evento. Siempre
     * cubre el evento, aunque sus jornadas hayan quedado desfasadas.
     */
    public static PeriodoReserva calcular(EventoLogistico evento, List<Jornada> jornadas) {
        LocalDateTime salida = extremo(jornadas, TipoJornada.MONTAJE, Periodo::inicio, Comparator.naturalOrder())
                .orElse(evento.fechaInicio());
        LocalDateTime retorno = extremo(jornadas, TipoJornada.DESMONTAJE, Periodo::fin, Comparator.reverseOrder())
                .orElse(evento.fechaFin());
        return new PeriodoReserva(salida.isBefore(evento.fechaInicio()) ? salida : evento.fechaInicio(),
                retorno.isAfter(evento.fechaFin()) ? retorno : evento.fechaFin());
    }

    public boolean seSuperponeCon(PeriodoReserva otro) {
        return salida.isBefore(otro.retorno) && otro.salida.isBefore(retorno);
    }

    public boolean incluye(LocalDateTime instante) {
        return !instante.isBefore(salida) && instante.isBefore(retorno);
    }

    private static Optional<LocalDateTime> extremo(List<Jornada> jornadas, TipoJornada tipo,
                                                             Function<Periodo, LocalDateTime> fecha,
                                                             Comparator<LocalDateTime> orden) {
        return jornadas.stream().filter(jornada -> jornada.tipo() == tipo).map(Jornada::periodo).map(fecha)
                .min(orden);
    }
}
