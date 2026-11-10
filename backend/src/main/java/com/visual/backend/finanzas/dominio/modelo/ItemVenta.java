package com.visual.backend.finanzas.dominio.modelo;

/**
 * Lo que sale del inventario en una venta: una unidad (idActivo) o una
 * cantidad de una existencia por lote (idExistencia).
 */
public record ItemVenta(Integer idActivo, Integer idExistencia, int cantidad, String descripcion) {

    public static ItemVenta unidad(Integer idActivo, String descripcion) {
        return new ItemVenta(idActivo, null, 1, descripcion);
    }

    public static ItemVenta lote(Integer idExistencia, int cantidad, String descripcion) {
        return new ItemVenta(null, idExistencia, cantidad, descripcion);
    }

    public boolean esUnidad() {
        return idActivo != null;
    }
}
