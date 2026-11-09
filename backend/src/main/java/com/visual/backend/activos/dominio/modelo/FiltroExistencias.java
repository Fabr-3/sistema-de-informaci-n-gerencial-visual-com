package com.visual.backend.activos.dominio.modelo;

/** Criterios opcionales de consulta de existencias por lote. */
public record FiltroExistencias(String texto, Integer idCategoria, Integer idBodega) {
}
