package com.visual.backend.finanzas.dominio.modelo;

import java.time.LocalDate;

/** Criterios de busqueda del modulo financiero. */
public final class Filtros {

    private Filtros() {
    }

    /** El texto busca en la razon social, el NIT y los servicios. */
    public record Proveedores(String texto) {
    }

    /** Fechas incluidas; el texto busca en el concepto y el numero de factura. */
    public record Movimientos(LocalDate desde, LocalDate hasta, Naturaleza naturaleza, Integer idTipo,
                              Integer idProyecto, Integer idEvento, Integer idProveedor, EstadoMovimiento estado,
                              String texto) {
    }

    /** El texto busca en el proyecto y el cliente; el estado es el del proyecto. */
    public record ResultadosProyectos(String texto, String estado) {
    }
}
