package com.visual.backend.gerencial.dominio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

/**
 * Situacion de alerta calculada a partir del estado actual (activos, reservas
 * y consumibles). clave identifica la situacion concreta dentro de su tipo
 * (el activo, el mantenimiento, la reserva o el consumible) para persistirla.
 * Las alertas de stock no se refieren a un activo: su idActivo y codigo son nulos.
 */
public record AlertaActivo(TipoAlerta tipo, Severidad severidad, Integer idActivo, Integer idEvento, String clave,
                           String codigo, String modelo, String categoria, String detalle) {

    /** De la mas a la menos urgente; dentro de cada tipo, por codigo de activo. */
    public static final Comparator<AlertaActivo> POR_URGENCIA = Comparator.comparing(AlertaActivo::severidad)
            .thenComparing(AlertaActivo::tipo)
            .thenComparing(AlertaActivo::codigo, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(AlertaActivo::detalle);

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int DECIMALES_HORAS = 1;

    public static AlertaActivo mantenimientoPreventivo(ActivoAfectado activo, boolean vencido, BigDecimal horas,
                                                       int intervalo) {
        String detalle = horas(horas) + " h de uso desde el último mantenimiento preventivo; el intervalo es de "
                + intervalo + " h";
        return de(vencido ? TipoAlerta.MANTENIMIENTO_VENCIDO : TipoAlerta.MANTENIMIENTO_PROXIMO,
                activo, detalle);
    }

    public static AlertaActivo mantenimientoProlongado(ActivoAfectado activo, Integer idMantenimiento,
                                                       LocalDate inicio, LocalDate hoy) {
        return conClave(TipoAlerta.MANTENIMIENTO_PROLONGADO, activo, null, idMantenimiento,
                "En mantenimiento desde el " + inicio.format(FECHA) + " (" + dias(inicio, hoy) + ")");
    }

    public static AlertaActivo modulosDefectuosos(ActivoAfectado activo, int cantidad) {
        return de(TipoAlerta.MODULOS_DEFECTUOSOS, activo, cantidad == 1 ? "1 módulo LED defectuoso"
                : cantidad + " módulos LED defectuosos");
    }

    public static AlertaActivo fallasRecurrentes(ActivoAfectado activo, int correctivos) {
        return de(TipoAlerta.FALLAS_RECURRENTES, activo,
                correctivos + " mantenimientos correctivos en los últimos 12 meses");
    }

    /** ultimoUso es nulo si el equipo nunca registro uso. */
    public static AlertaActivo sinUso(ActivoAfectado activo, LocalDate ultimoUso, LocalDate hoy) {
        String detalle = ultimoUso == null ? "Nunca registró uso"
                : "Último uso el " + ultimoUso.format(FECHA) + " (hace " + dias(ultimoUso, hoy) + ")";
        return de(TipoAlerta.SIN_USO, activo, detalle);
    }

    /** Equipo reservado para un evento proximo que esta en mantenimiento. */
    public static AlertaActivo reservaEnRiesgo(ActivoAfectado activo, Integer idReserva, Integer idEvento,
                                               String evento, LocalDate salida) {
        return conClave(TipoAlerta.RESERVA_EN_RIESGO, activo, idEvento, idReserva, "Reservado para «" + evento
                + "» desde el " + salida.format(FECHA) + " y está en mantenimiento");
    }

    /** Consumible con el stock en su minimo o por debajo. */
    public static AlertaActivo stockBajo(Integer idConsumible, String consumible, BigDecimal stock,
                                         BigDecimal minimo, String unidad) {
        return new AlertaActivo(TipoAlerta.STOCK_BAJO, TipoAlerta.STOCK_BAJO.severidad(), null, null,
                String.valueOf(idConsumible), null, null, null, consumible + ": " + cantidad(stock) + " " + unidad
                + " en stock (mínimo " + cantidad(minimo) + ")");
    }

    /** La clave por defecto es el activo: una alerta de ese tipo por equipo. */
    private static AlertaActivo de(TipoAlerta tipo, ActivoAfectado activo, String detalle) {
        return conClave(tipo, activo, null, activo.idActivo(), detalle);
    }

    private static AlertaActivo conClave(TipoAlerta tipo, ActivoAfectado activo, Integer idEvento, Integer clave,
                                         String detalle) {
        return new AlertaActivo(tipo, tipo.severidad(), activo.idActivo(), idEvento, String.valueOf(clave),
                activo.codigo(), activo.modelo(), activo.categoria(), detalle);
    }

    private static String cantidad(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static String horas(BigDecimal horas) {
        return horas.setScale(DECIMALES_HORAS, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private static String dias(LocalDate desde, LocalDate hasta) {
        long dias = ChronoUnit.DAYS.between(desde, hasta);
        return dias == 1 ? "1 día" : dias + " días";
    }
}
