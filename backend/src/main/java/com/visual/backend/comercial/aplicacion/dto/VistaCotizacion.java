package com.visual.backend.comercial.aplicacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.visual.backend.comercial.dominio.modelo.Cotizacion;
import com.visual.backend.comercial.dominio.modelo.EstadoCotizacion;
import com.visual.backend.comercial.dominio.modelo.LineaCotizacion;
import com.visual.backend.comercial.dominio.modelo.ResumenCliente;

/** Cotizacion con sus importes calculados y el proyecto que genero (si fue aceptada). */
public record VistaCotizacion(Integer id, String numero, ResumenCliente cliente, String titulo, LocalDate fechaEmision,
                              LocalDate fechaValidez, LocalDate fechaEventoInicio, LocalDate fechaEventoFin,
                              EstadoCotizacion estado, String condiciones, List<Linea> lineas,
                              BigDecimal importeTotal, String resueltoPor, LocalDateTime fechaResolucion,
                              String motivoRechazo, Integer idProyecto, LocalDateTime fechaCreacion) {

    public record Linea(Integer idServicio, String servicio, String unidadMedida, int cantidad,
                        BigDecimal precioUnitario, BigDecimal descuento, BigDecimal subtotal) {

        static Linea de(LineaCotizacion linea) {
            return new Linea(linea.idServicio(), linea.servicio(), linea.unidadMedida(), linea.cantidad(),
                    linea.precioUnitario(), linea.descuento(), linea.subtotal());
        }
    }

    public static VistaCotizacion de(Cotizacion cotizacion, Integer idProyecto) {
        return new VistaCotizacion(cotizacion.getId(), cotizacion.getNumero(), cotizacion.getCliente(),
                cotizacion.getTitulo(), cotizacion.getFechaEmision(), cotizacion.getFechaValidez(),
                cotizacion.getFechaEventoInicio(), cotizacion.getFechaEventoFin(), cotizacion.getEstado(),
                cotizacion.getCondiciones(), cotizacion.getLineas().stream().map(Linea::de).toList(),
                cotizacion.importeTotal(), cotizacion.getNombreResolutor(), cotizacion.getFechaResolucion(),
                cotizacion.getMotivoRechazo(), idProyecto, cotizacion.getFechaCreacion());
    }

    public Map<String, Object> comoMapaAuditoria() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("numero", numero);
        mapa.put("cliente", cliente.razonSocial());
        mapa.put("titulo", titulo);
        mapa.put("estado", estado.name());
        mapa.put("fechaValidez", fechaValidez.toString());
        mapa.put("importeTotal", importeTotal);
        mapa.put("lineas", lineas.stream().map(l -> l.cantidad() + " x " + l.servicio() + " = " + l.subtotal()).toList());
        mapa.put("motivoRechazo", motivoRechazo);
        return mapa;
    }
}
