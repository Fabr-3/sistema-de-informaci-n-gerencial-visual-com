package com.visual.backend.logistica.dominio.modelo;

import java.time.LocalDateTime;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Compromiso de un activo con el plan logistico de un evento. No cambia el
 * estado operativo del activo: su disponibilidad se deduce de las reservas
 * activas. Una reserva liberada conserva la fecha y el motivo.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReservaActivo {

    static final int LONGITUD_MAXIMA_MOTIVO = 255;

    private Integer id;
    private Integer idPlan;
    private Integer idActivo;
    private PeriodoReserva periodo;
    private EstadoReserva estado;
    private LocalDateTime fechaLiberacion;
    private String motivoLiberacion;
    private Integer creadoPor;
    private LocalDateTime fechaCreacion;
    private Integer modificadoPor;
    private LocalDateTime fechaModificacion;

    public static ReservaActivo nueva(Integer idPlan, ActivoReservable activo, PeriodoReserva periodo, Integer actor,
                                      LocalDateTime ahora) {
        activo.validarReservable();
        return ReservaActivo.builder()
                .idPlan(idPlan)
                .idActivo(activo.id())
                .periodo(periodo)
                .estado(EstadoReserva.ACTIVA)
                .creadoPor(actor)
                .fechaCreacion(ahora)
                .build();
    }

    public boolean activa() {
        return estado == EstadoReserva.ACTIVA;
    }

    /** Ajusta el periodo cuando cambian las jornadas o el horario del evento. */
    public void reprogramar(PeriodoReserva nuevo, Integer actor, LocalDateTime ahora) {
        exigirActiva();
        periodo = nuevo;
        registrarModificacion(actor, ahora);
    }

    public void liberar(String motivo, Integer actor, LocalDateTime ahora) {
        exigirActiva();
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioExcepcion("Indique el motivo de la liberación");
        }
        String limpio = motivo.trim();
        if (limpio.length() > LONGITUD_MAXIMA_MOTIVO) {
            throw new ReglaNegocioExcepcion("El motivo no puede superar " + LONGITUD_MAXIMA_MOTIVO + " caracteres");
        }
        estado = EstadoReserva.LIBERADA;
        motivoLiberacion = limpio;
        fechaLiberacion = ahora;
        registrarModificacion(actor, ahora);
    }

    private void exigirActiva() {
        if (!activa()) {
            throw new ReglaNegocioExcepcion("La reserva ya fue liberada");
        }
    }

    private void registrarModificacion(Integer actor, LocalDateTime ahora) {
        modificadoPor = actor;
        fechaModificacion = ahora;
    }
}
