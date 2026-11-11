package com.visual.backend.finanzas.infraestructura.eventos;

import java.util.Locale;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.visual.backend.activos.aplicacion.evento.MantenimientoFinalizado;
import com.visual.backend.finanzas.aplicacion.servicio.ServicioMovimientos;

/**
 * Registra como egreso el costo de cada mantenimiento finalizado. Se ejecuta
 * en la misma transaccion: si el egreso falla, la finalizacion se revierte.
 */
@Component
public class RegistroCostosMantenimiento {

    private static final Locale BOLIVIA = Locale.forLanguageTag("es-BO");

    private final ServicioMovimientos movimientos;

    public RegistroCostosMantenimiento(ServicioMovimientos movimientos) {
        this.movimientos = movimientos;
    }

    @EventListener
    public void alFinalizar(MantenimientoFinalizado mantenimiento) {
        if (!mantenimiento.tieneCosto()) {
            return;
        }
        String concepto = "Mantenimiento " + mantenimiento.tipoMantenimiento().toLowerCase(BOLIVIA) + " · "
                + mantenimiento.codigoActivo();
        movimientos.registrarCostoMantenimiento(mantenimiento.idMantenimiento(), concepto, mantenimiento.costo(),
                mantenimiento.fechaFin().toLocalDate());
    }
}
