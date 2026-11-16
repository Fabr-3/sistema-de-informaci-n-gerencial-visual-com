package com.visual.backend.logistica.dominio.modelo;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/**
 * Datos del modulo de activos que necesita la reserva. vigente indica que
 * sigue en el inventario (ni de baja ni vendido).
 */
public record ActivoReservable(Integer id, String codigo, String modelo, String categoria, boolean porUnidad,
                               boolean vigente, boolean enMantenimiento) {

    /** Solo se reservan equipos por unidad que siguen en el inventario y no estan en mantenimiento. */
    public void validarReservable() {
        if (!porUnidad) {
            throw new ReglaNegocioExcepcion("Solo se reservan equipos controlados por unidad");
        }
        if (!vigente) {
            throw new ReglaNegocioExcepcion("El equipo " + codigo + " ya no forma parte del inventario");
        }
        if (enMantenimiento) {
            throw new ReglaNegocioExcepcion("El equipo " + codigo + " está en mantenimiento");
        }
    }
}
