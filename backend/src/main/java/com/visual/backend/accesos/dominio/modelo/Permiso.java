package com.visual.backend.accesos.dominio.modelo;

/** Permiso fijo del sistema agrupado por modulo funcional. */
public record Permiso(Integer id, String modulo, String nombre, String descripcion) {
}
