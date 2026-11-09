package com.visual.backend.activos.dominio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Situacion del mantenimiento preventivo segun las horas de uso acumuladas
 * desde el ultimo mantenimiento que reinicia el contador.
 */
public enum NivelAlertaMantenimiento {
    /** El modelo no define intervalo de mantenimiento por horas. */
    SIN_PLAN,
    AL_DIA,
    /** Se alcanzo el porcentaje de alerta del intervalo. */
    PROXIMO,
    /** Se supero el intervalo de horas. */
    VENCIDO;

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    public static NivelAlertaMantenimiento calcular(BigDecimal horasDesdeMantenimiento, Integer intervaloHoras,
                                                    int porcentajeAlerta) {
        if (intervaloHoras == null) {
            return SIN_PLAN;
        }
        BigDecimal intervalo = BigDecimal.valueOf(intervaloHoras);
        if (horasDesdeMantenimiento.compareTo(intervalo) >= 0) {
            return VENCIDO;
        }
        BigDecimal umbral = intervalo.multiply(BigDecimal.valueOf(porcentajeAlerta))
                .divide(CIEN, 2, RoundingMode.HALF_UP);
        return horasDesdeMantenimiento.compareTo(umbral) >= 0 ? PROXIMO : AL_DIA;
    }

    public boolean requiereAtencion() {
        return this == PROXIMO || this == VENCIDO;
    }
}
