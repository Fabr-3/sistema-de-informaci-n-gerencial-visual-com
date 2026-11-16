package com.visual.backend.subcontratacion.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.visual.backend.compartido.dominio.ElementoCatalogo;

/** servicio y evento son opcionales. */
public record DatosSubcontratacion(ElementoCatalogo proveedor, ElementoCatalogo servicio, ElementoCatalogo proyecto,
                                   ElementoCatalogo evento, String descripcion, BigDecimal costo, LocalDate fecha) {
}
