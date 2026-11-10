package com.visual.backend.activos.dominio.modelo;

/**
 * Tipos de movimiento de existencias por lote y su efecto. AJUSTE admite
 * ambos sentidos (conteo fisico); los traslados se registran en pares.
 */
public enum TipoMovimientoLote {
    INGRESO(1),
    SALIDA(-1),
    RETORNO(1),
    BAJA(-1),
    AJUSTE(0),
    TRASLADO_SALIDA(-1),
    TRASLADO_ENTRADA(1),
    VENTA(-1),
    ANULACION_VENTA(1);

    private final int signo;

    TipoMovimientoLote(int signo) {
        this.signo = signo;
    }

    /** Las ventas y sus anulaciones las registra el modulo financiero. */
    public boolean esDeVenta() {
        return this == VENTA || this == ANULACION_VENTA;
    }

    /** Los traslados se registran mediante la operacion de traslado, no individualmente. */
    public boolean esTraslado() {
        return this == TRASLADO_SALIDA || this == TRASLADO_ENTRADA;
    }

    /**
     * Cantidad con signo que se aplica a la existencia. Para AJUSTE se respeta
     * el signo recibido; para los demas, la cantidad debe ser positiva.
     */
    public int cantidadConSigno(int cantidad) {
        if (signo == 0) {
            return cantidad;
        }
        return signo * Math.abs(cantidad);
    }
}
