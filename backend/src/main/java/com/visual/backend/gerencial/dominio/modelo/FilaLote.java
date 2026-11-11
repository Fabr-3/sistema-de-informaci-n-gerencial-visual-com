package com.visual.backend.gerencial.dominio.modelo;

/** Existencia de un modelo controlado por lote en una bodega. */
public record FilaLote(String modelo, String categoria, String bodega, int cantidad) {
}
