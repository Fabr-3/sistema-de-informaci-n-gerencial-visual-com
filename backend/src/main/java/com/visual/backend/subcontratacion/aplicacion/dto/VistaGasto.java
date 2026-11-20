package com.visual.backend.subcontratacion.aplicacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.subcontratacion.dominio.modelo.EstadoConsumo;
import com.visual.backend.subcontratacion.dominio.modelo.GastoEvento;

/** El comprobante se descarga aparte; aqui solo va su nombre y tipo. */
public record VistaGasto(Integer id, ElementoCatalogo proyecto, ElementoCatalogo evento, String descripcion,
                         BigDecimal monto, LocalDate fecha, String comprobante, String tipoComprobante,
                         EstadoConsumo estado, String motivoAnulacion, Integer idMovimiento, String registradoPor) {

    public static VistaGasto de(GastoEvento gasto) {
        return new VistaGasto(gasto.getId(), gasto.getProyecto(), gasto.getEvento(), gasto.getDescripcion(),
                gasto.getMonto(), gasto.getFecha(), gasto.getComprobante().nombreOriginal(),
                gasto.getComprobante().tipoContenido(), gasto.getEstado(), gasto.getMotivoAnulacion(),
                gasto.getIdMovimiento(), gasto.getNombreRegistrador());
    }

    public static Map<String, Object> mapaAuditoria(GastoEvento gasto) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("proyecto", gasto.getProyecto().nombre());
        mapa.put("evento", gasto.getEvento().nombre());
        mapa.put("descripcion", gasto.getDescripcion());
        mapa.put("monto", gasto.getMonto().toPlainString());
        mapa.put("fecha", gasto.getFecha().toString());
        mapa.put("comprobante", gasto.getComprobante().nombreOriginal());
        mapa.put("estado", gasto.getEstado().name());
        mapa.put("motivoAnulacion", gasto.getMotivoAnulacion());
        mapa.put("egreso", gasto.getIdMovimiento());
        return mapa;
    }
}
