package com.visual.backend.finanzas.dominio.puerto;

/**
 * Origen de los movimientos registrados por otros modulos, para que finanzas
 * no dependa de ellos al validar una anulacion.
 */
public interface ConsultaOrigenMovimiento {

    /** El egreso pertenece a una subcontratacion o a un gasto del evento: se anula desde ese modulo. */
    boolean esDeSubcontratacion(Integer idMovimiento);
}
