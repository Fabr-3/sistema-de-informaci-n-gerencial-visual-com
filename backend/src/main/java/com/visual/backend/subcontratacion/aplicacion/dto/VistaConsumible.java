package com.visual.backend.subcontratacion.aplicacion.dto;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import com.visual.backend.subcontratacion.dominio.modelo.Consumible;

public record VistaConsumible(Integer id, String nombre, String unidadMedida, BigDecimal stockActual,
                              BigDecimal stockMinimo, BigDecimal costoUnitario, boolean activo, boolean stockBajo) {

    public static VistaConsumible de(Consumible consumible) {
        return new VistaConsumible(consumible.getId(), consumible.getNombre(), consumible.getUnidadMedida(),
                consumible.getStockActual(), consumible.getStockMinimo(), consumible.getCostoUnitario(),
                consumible.isActivo(), consumible.stockBajo());
    }

    public static Map<String, Object> mapaAuditoria(Consumible consumible) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("nombre", consumible.getNombre());
        mapa.put("unidadMedida", consumible.getUnidadMedida());
        mapa.put("stockActual", consumible.getStockActual().toPlainString());
        mapa.put("stockMinimo", consumible.getStockMinimo() == null ? null
                : consumible.getStockMinimo().toPlainString());
        mapa.put("costoUnitario", consumible.getCostoUnitario().toPlainString());
        mapa.put("activo", consumible.isActivo());
        return mapa;
    }
}
