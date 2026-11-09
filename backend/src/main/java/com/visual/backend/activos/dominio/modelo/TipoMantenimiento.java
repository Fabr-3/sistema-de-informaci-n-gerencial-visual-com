package com.visual.backend.activos.dominio.modelo;

/** reiniciaHoras: al finalizar, reinicia el contador del mantenimiento preventivo. */
public record TipoMantenimiento(Integer id, String nombre, boolean reiniciaHoras) {
}
