package com.visual.backend.accesos.dominio.modelo;

/** Criterios opcionales de busqueda de usuarios (nulo = sin filtro). */
public record FiltroUsuarios(String texto, Integer idRol, Boolean activo) {
}
