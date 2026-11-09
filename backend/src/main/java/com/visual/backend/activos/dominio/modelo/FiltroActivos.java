package com.visual.backend.activos.dominio.modelo;

/**
 * Criterios opcionales de busqueda de activos (nulo = sin filtro).
 * soloAlertas: solo activos que alcanzaron el umbral de mantenimiento.
 */
public record FiltroActivos(String texto, Integer idCategoria, Integer idModelo, Integer idBodega, EstadoOperativo estado,
                            boolean soloAlertas) {
}
