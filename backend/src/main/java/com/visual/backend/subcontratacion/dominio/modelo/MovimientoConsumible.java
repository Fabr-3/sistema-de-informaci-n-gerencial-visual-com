package com.visual.backend.subcontratacion.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Cambio del stock de un consumible; la cantidad lleva signo (negativa si lo reduce). */
public record MovimientoConsumible(Integer id, Integer idConsumible, TipoMovimientoConsumible tipo,
                                   BigDecimal cantidad, LocalDateTime fecha, String motivo, Integer creadoPor,
                                   String nombreCreador) {

    static final int LONGITUD_MAXIMA_MOTIVO = 255;

    /** Un motivo demasiado largo (p. ej. el nombre de un evento extenso) se recorta. */
    public static MovimientoConsumible nuevo(Integer idConsumible, TipoMovimientoConsumible tipo, BigDecimal cantidad,
                                             String motivo, Integer actor, LocalDateTime ahora) {
        String recortado = motivo == null || motivo.length() <= LONGITUD_MAXIMA_MOTIVO ? motivo
                : motivo.substring(0, LONGITUD_MAXIMA_MOTIVO);
        return new MovimientoConsumible(null, idConsumible, tipo, cantidad, ahora, recortado, actor, null);
    }
}
