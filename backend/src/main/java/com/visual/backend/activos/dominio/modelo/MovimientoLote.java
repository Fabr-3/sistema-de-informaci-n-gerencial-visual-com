package com.visual.backend.activos.dominio.modelo;

import java.time.LocalDateTime;

/** Movimiento historico de una existencia por lote (cantidad con signo). */
public record MovimientoLote(Integer id, Integer idExistencia, TipoMovimientoLote tipo, int cantidad, String motivo,
                             LocalDateTime fecha, Integer creadoPor, String nombreCreador) {
}
