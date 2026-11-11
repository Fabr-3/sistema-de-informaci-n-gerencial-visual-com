package com.visual.backend.finanzas.dominio.modelo;

/**
 * Clase de movimiento (cobro, venta, compra, subcontratacion...). Define si
 * exige proveedor y si descuenta inventario (ventas de equipos). Los tipos
 * con codigo de sistema solo los registra la aplicacion.
 */
public record TipoMovimiento(Integer id, String nombre, Naturaleza naturaleza, boolean requiereProveedor,
                             boolean descuentaInventario, String codigoSistema) {

    /** Egreso que se registra al finalizar un mantenimiento con costo. */
    public static final String CODIGO_MANTENIMIENTO = "MANTENIMIENTO";

    public boolean esAutomatico() {
        return codigoSistema != null;
    }
}
