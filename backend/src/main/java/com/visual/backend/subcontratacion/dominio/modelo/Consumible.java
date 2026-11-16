package com.visual.backend.subcontratacion.dominio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Material que se gasta en los eventos (cinta, baterias, gaffer...). Su stock
 * nunca queda negativo y cada cambio se registra como movimiento.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Consumible {

    public static final int DECIMALES = 2;
    static final int LONGITUD_MAXIMA_NOMBRE = 120;
    static final int LONGITUD_MAXIMA_UNIDAD = 30;

    private Integer id;
    private String nombre;
    private String unidadMedida;
    private BigDecimal stockActual;
    private BigDecimal stockMinimo;
    private BigDecimal costoUnitario;
    private boolean activo;
    private Integer creadoPor;
    private LocalDateTime fechaCreacion;
    private Integer modificadoPor;
    private LocalDateTime fechaModificacion;

    public static Consumible nuevo(DatosConsumible datos, Integer actor, LocalDateTime ahora) {
        Consumible consumible = Consumible.builder()
                .stockActual(BigDecimal.ZERO.setScale(DECIMALES))
                .creadoPor(actor)
                .fechaCreacion(ahora)
                .build();
        consumible.aplicar(datos);
        return consumible;
    }

    public void actualizar(DatosConsumible datos, Integer actor, LocalDateTime ahora) {
        aplicar(datos);
        registrarModificacion(actor, ahora);
    }

    /** Stock en el minimo o por debajo: se advierte en el listado y en las alertas. */
    public boolean stockBajo() {
        return stockMinimo != null && stockActual.compareTo(stockMinimo) <= 0;
    }

    /** Ingreso por compra o reposicion; un costo nuevo reemplaza al vigente para los consumos siguientes. */
    public MovimientoConsumible ingresar(BigDecimal cantidad, BigDecimal nuevoCosto, String motivo, Integer actor,
                                         LocalDateTime ahora) {
        BigDecimal positiva = exigirPositiva(cantidad);
        if (nuevoCosto != null) {
            costoUnitario = exigirCosto(nuevoCosto);
        }
        return mover(TipoMovimientoConsumible.INGRESO, positiva, motivo, actor, ahora);
    }

    /** Correccion por conteo fisico; exige motivo y no deja el stock en negativo. */
    public MovimientoConsumible ajustar(BigDecimal cantidad, String motivo, Integer actor, LocalDateTime ahora) {
        if (cantidad == null || cantidad.signum() == 0) {
            throw new ReglaNegocioExcepcion("Indique una cantidad distinta de cero para el ajuste");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioExcepcion("Indique el motivo del ajuste");
        }
        BigDecimal redondeada = redondear(cantidad);
        if (stockActual.add(redondeada).signum() < 0) {
            throw new ReglaNegocioExcepcion("El ajuste dejaría el stock de " + nombre + " en negativo (stock actual: "
                    + stockActual.toPlainString() + " " + unidadMedida + ")");
        }
        return mover(TipoMovimientoConsumible.AJUSTE, redondeada, motivo.trim(), actor, ahora);
    }

    /** Descuenta lo consumido en un evento; falla si no alcanza el stock. */
    public MovimientoConsumible consumir(BigDecimal cantidad, String referencia, Integer actor, LocalDateTime ahora) {
        BigDecimal positiva = exigirPositiva(cantidad);
        if (!activo) {
            throw new ReglaNegocioExcepcion("El consumible " + nombre + " está inactivo");
        }
        if (positiva.compareTo(stockActual) > 0) {
            throw new ReglaNegocioExcepcion("Stock insuficiente de " + nombre + ": disponible "
                    + stockActual.toPlainString() + " " + unidadMedida);
        }
        return mover(TipoMovimientoConsumible.CONSUMO, positiva.negate(), referencia, actor, ahora);
    }

    /** Devuelve al stock lo de un consumo anulado. */
    public MovimientoConsumible devolver(BigDecimal cantidad, String referencia, Integer actor, LocalDateTime ahora) {
        return mover(TipoMovimientoConsumible.ANULACION, exigirPositiva(cantidad), referencia, actor, ahora);
    }

    private MovimientoConsumible mover(TipoMovimientoConsumible tipo, BigDecimal cantidad, String motivo,
                                       Integer actor, LocalDateTime ahora) {
        stockActual = stockActual.add(cantidad);
        registrarModificacion(actor, ahora);
        return MovimientoConsumible.nuevo(id, tipo, cantidad, motivo == null || motivo.isBlank() ? null
                : motivo.trim(), actor, ahora);
    }

    private void aplicar(DatosConsumible datos) {
        nombre = texto(datos.nombre(), "el nombre", LONGITUD_MAXIMA_NOMBRE);
        unidadMedida = texto(datos.unidadMedida(), "la unidad de medida", LONGITUD_MAXIMA_UNIDAD);
        if (datos.stockMinimo() != null && datos.stockMinimo().signum() < 0) {
            throw new ReglaNegocioExcepcion("El stock mínimo no puede ser negativo");
        }
        stockMinimo = datos.stockMinimo() == null ? null : redondear(datos.stockMinimo());
        costoUnitario = exigirCosto(datos.costoUnitario());
        activo = datos.activo();
    }

    private void registrarModificacion(Integer actor, LocalDateTime ahora) {
        if (id != null) {
            modificadoPor = actor;
            fechaModificacion = ahora;
        }
    }

    private static BigDecimal exigirPositiva(BigDecimal cantidad) {
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new ReglaNegocioExcepcion("La cantidad debe ser mayor a cero");
        }
        return redondear(cantidad);
    }

    private static BigDecimal exigirCosto(BigDecimal costo) {
        if (costo == null || costo.signum() < 0) {
            throw new ReglaNegocioExcepcion("El costo unitario no puede ser negativo");
        }
        return redondear(costo);
    }

    private static String texto(String valor, String campo, int longitudMaxima) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioExcepcion("Indique " + campo + " del consumible");
        }
        String limpio = valor.trim();
        if (limpio.length() > longitudMaxima) {
            throw new ReglaNegocioExcepcion("El campo " + campo + " no puede superar " + longitudMaxima
                    + " caracteres");
        }
        return limpio;
    }

    private static BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(DECIMALES, RoundingMode.HALF_UP);
    }
}
