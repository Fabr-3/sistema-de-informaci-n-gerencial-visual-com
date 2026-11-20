package com.visual.backend.subcontratacion.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.visual.backend.compartido.dominio.ElementoCatalogo;

public record DatosGasto(ElementoCatalogo proyecto, ElementoCatalogo evento, String descripcion, BigDecimal monto,
                         LocalDate fecha, Comprobante comprobante) {
}
