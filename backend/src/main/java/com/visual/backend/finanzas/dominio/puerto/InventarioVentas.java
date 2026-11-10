package com.visual.backend.finanzas.dominio.puerto;

/**
 * Efecto de las ventas sobre el inventario del modulo de activos. Las
 * operaciones fallan si el equipo no esta disponible o falta existencia.
 */
public interface InventarioVentas {

    void venderUnidad(Integer idActivo, String referencia);

    void venderLote(Integer idExistencia, int cantidad, String referencia);

    void revertirUnidad(Integer idActivo, String referencia);

    void revertirLote(Integer idExistencia, int cantidad, String referencia);
}
