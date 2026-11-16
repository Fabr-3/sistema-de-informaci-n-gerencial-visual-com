package com.visual.backend.subcontratacion.aplicacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.subcontratacion.dominio.modelo.ConsumoProyecto;
import com.visual.backend.subcontratacion.dominio.modelo.EstadoConsumo;

public record VistaConsumo(Integer id, ElementoCatalogo consumible, String unidadMedida, ElementoCatalogo proyecto,
                           ElementoCatalogo evento, BigDecimal cantidad, BigDecimal costoUnitario,
                           BigDecimal costoTotal, LocalDate fecha, EstadoConsumo estado, String motivoAnulacion,
                           String registradoPor) {

    public static VistaConsumo de(ConsumoProyecto consumo) {
        return new VistaConsumo(consumo.getId(), consumo.getConsumible(), consumo.getUnidadMedida(),
                consumo.getProyecto(), consumo.getEvento(), consumo.getCantidad(), consumo.getCostoUnitario(),
                consumo.costoTotal(), consumo.getFecha(), consumo.getEstado(), consumo.getMotivoAnulacion(),
                consumo.getNombreRegistrador());
    }

    public static Map<String, Object> mapaAuditoria(ConsumoProyecto consumo) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("consumible", consumo.getConsumible().nombre());
        mapa.put("proyecto", consumo.getProyecto().nombre());
        mapa.put("evento", consumo.getEvento() == null ? null : consumo.getEvento().nombre());
        mapa.put("cantidad", consumo.getCantidad().toPlainString());
        mapa.put("costoUnitario", consumo.getCostoUnitario().toPlainString());
        mapa.put("fecha", consumo.getFecha().toString());
        mapa.put("estado", consumo.getEstado().name());
        mapa.put("motivoAnulacion", consumo.getMotivoAnulacion());
        return mapa;
    }
}
