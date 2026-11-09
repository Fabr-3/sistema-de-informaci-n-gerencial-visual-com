package com.visual.backend.comercial.dominio.modelo;

import java.time.LocalDate;
import java.util.List;

/** Contenido editable de una cotizacion. */
public record DatosCotizacion(ResumenCliente cliente, String titulo, LocalDate fechaEmision, LocalDate fechaValidez,
                              LocalDate fechaEventoInicio, LocalDate fechaEventoFin, String condiciones,
                              List<LineaCotizacion> lineas) {
}
