package com.visual.backend.comercial.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Historia comercial de un cliente: cotizaciones y proyectos por estado,
 * importe cotizado (todas las cotizaciones) y contratado (las aceptadas), y
 * las ultimas cotizaciones y proyectos.
 */
public record ResumenComercialCliente(Integer idCliente, Map<String, Long> cotizacionesPorEstado,
                                      BigDecimal importeCotizado, BigDecimal importeContratado,
                                      Map<String, Long> proyectosPorEstado, List<Reciente> ultimasCotizaciones,
                                      List<Reciente> ultimosProyectos) {

    /** Cotizacion (por su numero) o proyecto (por su nombre) reciente. */
    public record Reciente(Integer id, String referencia, String estado, LocalDate fecha) {
    }
}
