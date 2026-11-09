package com.visual.backend.activos.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Horas de uso acumuladas de un activo (dato derivado del historial). */
public record UsoActivo(BigDecimal horasTotales, BigDecimal horasDesdeMantenimiento,
                        LocalDateTime fechaUltimoMantenimiento) {

    public static final UsoActivo SIN_USO = new UsoActivo(BigDecimal.ZERO, BigDecimal.ZERO, null);
}
