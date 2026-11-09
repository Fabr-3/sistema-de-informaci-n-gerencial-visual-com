package com.visual.backend.comercial.dominio.modelo;

import java.math.BigDecimal;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/**
 * Servicio cotizado. El precio unitario es el negociado en esta cotizacion
 * (no el de catalogo) y el descuento es un monto en Bs sobre la linea.
 */
public record LineaCotizacion(Integer idServicio, String servicio, String unidadMedida, int cantidad,
                              BigDecimal precioUnitario, BigDecimal descuento) {

    public LineaCotizacion {
        descuento = descuento == null ? BigDecimal.ZERO : descuento;
        if (cantidad <= 0) {
            throw new ReglaNegocioExcepcion("La cantidad de " + servicio + " debe ser mayor a cero");
        }
        if (precioUnitario == null || precioUnitario.signum() < 0 || descuento.signum() < 0) {
            throw new ReglaNegocioExcepcion("El precio y el descuento de " + servicio + " no pueden ser negativos");
        }
        if (descuento.compareTo(bruto(cantidad, precioUnitario)) > 0) {
            throw new ReglaNegocioExcepcion("El descuento de " + servicio + " supera el importe de la línea");
        }
    }

    public BigDecimal subtotal() {
        return bruto(cantidad, precioUnitario).subtract(descuento);
    }

    private static BigDecimal bruto(int cantidad, BigDecimal precioUnitario) {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
