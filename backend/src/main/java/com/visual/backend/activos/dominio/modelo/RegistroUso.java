package com.visual.backend.activos.dominio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/**
 * Periodo de uso de un activo. Es un registro historico: no puede terminar
 * en el futuro. Los registros erroneos se anulan (borrado logico).
 */
public record RegistroUso(Integer id, Integer idActivo, LocalDateTime fechaInicio, LocalDateTime fechaFin,
                          String descripcion, Integer creadoPor, String nombreCreador,
                          LocalDateTime fechaCreacion) {

    private static final int ESCALA_HORAS = 2;
    private static final BigDecimal SEGUNDOS_POR_HORA = BigDecimal.valueOf(3600);

    public static RegistroUso nuevo(Integer idActivo, LocalDateTime inicio, LocalDateTime fin, String descripcion,
                                    Integer actor, LocalDateTime ahora) {
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            throw new ReglaNegocioExcepcion("La fecha de fin del uso debe ser posterior a la de inicio");
        }
        if (fin.isAfter(ahora)) {
            throw new ReglaNegocioExcepcion("Solo se registran usos ya realizados: la fecha de fin no puede ser futura");
        }
        String texto = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
        return new RegistroUso(null, idActivo, inicio, fin, texto, actor, null, ahora);
    }

    public BigDecimal horas() {
        BigDecimal segundos = BigDecimal.valueOf(Duration.between(fechaInicio, fechaFin).toSeconds());
        return segundos.divide(SEGUNDOS_POR_HORA, ESCALA_HORAS, RoundingMode.HALF_UP);
    }
}
