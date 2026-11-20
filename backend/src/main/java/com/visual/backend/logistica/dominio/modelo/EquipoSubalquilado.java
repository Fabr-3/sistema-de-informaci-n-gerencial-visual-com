package com.visual.backend.logistica.dominio.modelo;

import java.math.BigDecimal;

/** Unidades de un modelo alquiladas a un proveedor para el evento (subcontratacion vigente). */
public record EquipoSubalquilado(Integer idSubcontratacion, String modelo, int cantidad, String proveedor,
                                 BigDecimal costo) {
}
