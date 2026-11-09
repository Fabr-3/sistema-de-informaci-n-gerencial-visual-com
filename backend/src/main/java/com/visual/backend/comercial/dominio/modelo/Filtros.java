package com.visual.backend.comercial.dominio.modelo;

import java.time.LocalDateTime;

/** Criterios opcionales de busqueda del modulo comercial (nulo = sin filtro). */
public final class Filtros {

    private Filtros() {
    }

    public record Clientes(String texto, Integer idTipoCliente) {
    }

    public record Cotizaciones(String texto, EstadoCotizacion estado, Integer idCliente) {
    }

    public record Proyectos(String texto, EstadoProyecto estado, Integer idCliente) {
    }

    /** Eventos que se cruzan con el rango [desde, hasta). */
    public record Eventos(LocalDateTime desde, LocalDateTime hasta, Integer idProyecto) {
    }
}
