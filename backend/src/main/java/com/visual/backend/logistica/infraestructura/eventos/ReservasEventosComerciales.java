package com.visual.backend.logistica.infraestructura.eventos;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.visual.backend.comercial.aplicacion.evento.EventoCancelado;
import com.visual.backend.comercial.aplicacion.evento.EventoReprogramado;
import com.visual.backend.logistica.aplicacion.servicio.ServicioReservas;

/**
 * Mantiene las reservas de equipos al dia con los cambios del evento. Se
 * ejecuta en la misma transaccion: si la reprogramacion deja una reserva en
 * conflicto, el cambio del evento se revierte.
 */
@Component
public class ReservasEventosComerciales {

    private final ServicioReservas reservas;

    public ReservasEventosComerciales(ServicioReservas reservas) {
        this.reservas = reservas;
    }

    @EventListener
    public void alReprogramar(EventoReprogramado evento) {
        reservas.recalcularEvento(evento.idEvento());
    }

    @EventListener
    public void alCancelar(EventoCancelado evento) {
        reservas.liberarPorCancelacion(evento.idEvento());
    }
}
