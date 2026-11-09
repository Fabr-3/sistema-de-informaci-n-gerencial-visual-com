package com.visual.backend.comercial.dominio.modelo;

import java.math.BigDecimal;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/** Servicio ofrecido a los clientes con su precio referencial en Bs por unidad de medida. */
public record Servicio(Integer id, String nombre, String descripcion, String unidadMedida,
                       BigDecimal precioReferencial, boolean activo) {

    public Servicio {
        nombre = nombre == null ? null : nombre.trim();
        unidadMedida = unidadMedida == null ? null : unidadMedida.trim();
        descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
        if (precioReferencial != null && precioReferencial.signum() < 0) {
            throw new ReglaNegocioExcepcion("El precio referencial no puede ser negativo");
        }
    }
}
