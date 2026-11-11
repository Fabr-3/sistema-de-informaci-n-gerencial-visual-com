package com.visual.backend.activos.aplicacion.evento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Se publica dentro de la transaccion que finaliza un mantenimiento, para
 * que otros modulos (finanzas) registren sus efectos. costo puede ser nulo.
 */
public record MantenimientoFinalizado(Integer idMantenimiento, String codigoActivo, String tipoMantenimiento,
                                      LocalDateTime fechaFin, BigDecimal costo) {

    public boolean tieneCosto() {
        return costo != null && costo.signum() > 0;
    }
}
