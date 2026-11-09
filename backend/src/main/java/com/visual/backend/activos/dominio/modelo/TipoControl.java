package com.visual.backend.activos.dominio.modelo;

/**
 * Forma de controlar el inventario de un modelo: UNIDAD (cada equipo con
 * codigo y serie propios, horas de uso y mantenimiento) o LOTE (cantidades
 * por bodega, p. ej. cables y accesorios).
 */
public enum TipoControl {
    UNIDAD,
    LOTE
}
