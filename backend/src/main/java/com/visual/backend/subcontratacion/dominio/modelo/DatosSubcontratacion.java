package com.visual.backend.subcontratacion.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.visual.backend.compartido.dominio.ElementoCatalogo;

/**
 * servicio y evento son opcionales. modelo y cantidad solo en un subalquiler
 * de equipos, que exige el evento.
 */
public record DatosSubcontratacion(ElementoCatalogo proveedor, ElementoCatalogo servicio, ElementoCatalogo proyecto,
                                   ElementoCatalogo evento, String descripcion, BigDecimal costo, LocalDate fecha,
                                   ElementoCatalogo modelo, Integer cantidad) {

    public DatosSubcontratacion(ElementoCatalogo proveedor, ElementoCatalogo servicio, ElementoCatalogo proyecto,
                                ElementoCatalogo evento, String descripcion, BigDecimal costo, LocalDate fecha) {
        this(proveedor, servicio, proyecto, evento, descripcion, costo, fecha, null, null);
    }
}
