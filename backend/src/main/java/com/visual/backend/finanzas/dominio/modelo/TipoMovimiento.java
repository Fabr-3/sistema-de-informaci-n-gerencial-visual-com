package com.visual.backend.finanzas.dominio.modelo;

/**
 * Clase de movimiento (cobro, venta, compra, subcontratacion...). Define si
 * exige proveedor y si descuenta inventario (ventas de equipos).
 */
public record TipoMovimiento(Integer id, String nombre, Naturaleza naturaleza, boolean requiereProveedor,
                             boolean descuentaInventario) {
}
