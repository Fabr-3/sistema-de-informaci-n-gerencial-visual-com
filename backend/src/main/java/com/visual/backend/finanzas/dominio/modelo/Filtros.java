package com.visual.backend.finanzas.dominio.modelo;

/** Criterios de busqueda del modulo financiero. */
public final class Filtros {

    private Filtros() {
    }

    /** El texto busca en la razon social, el NIT y los servicios. */
    public record Proveedores(String texto) {
    }
}
