package com.visual.backend.logistica.dominio.modelo;

import java.time.LocalDateTime;

/** Criterios de busqueda del modulo de logistica. */
public final class Filtros {

    private Filtros() {
    }

    /** Eventos que se cruzan con [desde, hasta); el texto busca en evento, proyecto y cliente. */
    public record Eventos(LocalDateTime desde, LocalDateTime hasta, String texto) {
    }

    public record Personal(String texto, Boolean activo, Integer idEspecialidad) {
    }
}
