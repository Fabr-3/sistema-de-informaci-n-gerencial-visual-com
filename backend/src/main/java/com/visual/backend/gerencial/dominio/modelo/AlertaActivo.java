package com.visual.backend.gerencial.dominio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

/** Alerta sobre el estado de un activo, con su detalle legible. */
public record AlertaActivo(TipoAlertaActivo tipo, Severidad severidad, Integer idActivo, String codigo,
                           String modelo, String categoria, String detalle) {

    /** De la mas a la menos urgente; dentro de cada tipo, por codigo de activo. */
    public static final Comparator<AlertaActivo> POR_URGENCIA = Comparator.comparing(AlertaActivo::severidad)
            .thenComparing(AlertaActivo::tipo).thenComparing(AlertaActivo::codigo);

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int DECIMALES_HORAS = 1;

    public static AlertaActivo mantenimientoPreventivo(ActivoAfectado activo, boolean vencido, BigDecimal horas,
                                                       int intervalo) {
        String detalle = horas(horas) + " h de uso desde el último mantenimiento preventivo; el intervalo es de "
                + intervalo + " h";
        return de(vencido ? TipoAlertaActivo.MANTENIMIENTO_VENCIDO : TipoAlertaActivo.MANTENIMIENTO_PROXIMO,
                activo, detalle);
    }

    public static AlertaActivo mantenimientoProlongado(ActivoAfectado activo, LocalDate inicio, LocalDate hoy) {
        return de(TipoAlertaActivo.MANTENIMIENTO_PROLONGADO, activo, "En mantenimiento desde el "
                + inicio.format(FECHA) + " (" + dias(inicio, hoy) + ")");
    }

    public static AlertaActivo modulosDefectuosos(ActivoAfectado activo, int cantidad) {
        return de(TipoAlertaActivo.MODULOS_DEFECTUOSOS, activo, cantidad == 1 ? "1 módulo LED defectuoso"
                : cantidad + " módulos LED defectuosos");
    }

    public static AlertaActivo fallasRecurrentes(ActivoAfectado activo, int correctivos) {
        return de(TipoAlertaActivo.FALLAS_RECURRENTES, activo,
                correctivos + " mantenimientos correctivos en los últimos 12 meses");
    }

    /** ultimoUso es nulo si el equipo nunca registro uso. */
    public static AlertaActivo sinUso(ActivoAfectado activo, LocalDate ultimoUso, LocalDate hoy) {
        String detalle = ultimoUso == null ? "Nunca registró uso"
                : "Último uso el " + ultimoUso.format(FECHA) + " (hace " + dias(ultimoUso, hoy) + ")";
        return de(TipoAlertaActivo.SIN_USO, activo, detalle);
    }

    private static AlertaActivo de(TipoAlertaActivo tipo, ActivoAfectado activo, String detalle) {
        return new AlertaActivo(tipo, tipo.severidad(), activo.idActivo(), activo.codigo(), activo.modelo(),
                activo.categoria(), detalle);
    }

    private static String horas(BigDecimal horas) {
        return horas.setScale(DECIMALES_HORAS, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private static String dias(LocalDate desde, LocalDate hasta) {
        long dias = ChronoUnit.DAYS.between(desde, hasta);
        return dias == 1 ? "1 día" : dias + " días";
    }
}
