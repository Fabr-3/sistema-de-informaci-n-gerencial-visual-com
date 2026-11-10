package com.visual.backend.finanzas.dominio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Ingreso o egreso de dinero. Se imputa opcionalmente a un proyecto y a uno
 * de sus eventos. No se edita: un error se corrige anulando y registrando
 * de nuevo, para conservar la trazabilidad.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MovimientoFinanciero {

    /** Montos en bolivianos con centavos. */
    public static final int DECIMALES = 2;
    private static final String PREFIJO_REFERENCIA = "Movimiento financiero N.º ";

    private Integer id;
    private TipoMovimiento tipo;
    private String concepto;
    private BigDecimal monto;
    private LocalDate fecha;
    private Referencia proyecto;
    private Referencia evento;
    private Referencia proveedor;
    private String numeroFactura;
    private EstadoMovimiento estado;
    private String motivoAnulacion;
    private List<ItemVenta> items;
    private Integer creadoPor;
    private String nombreCreador;
    private LocalDateTime fechaCreacion;
    private Integer modificadoPor;
    private LocalDateTime fechaModificacion;

    public static MovimientoFinanciero registrar(DatosMovimiento datos, Integer actor, LocalDateTime ahora) {
        validar(datos, ahora.toLocalDate());
        return MovimientoFinanciero.builder()
                .tipo(datos.tipo())
                .concepto(datos.concepto().trim())
                .monto(datos.monto().setScale(DECIMALES, RoundingMode.HALF_UP))
                .fecha(datos.fecha())
                .proyecto(datos.proyecto())
                .evento(datos.evento())
                .proveedor(datos.proveedor())
                .numeroFactura(textoOpcional(datos.numeroFactura()))
                .estado(EstadoMovimiento.REGISTRADO)
                .items(datos.items() == null ? List.of() : List.copyOf(datos.items()))
                .creadoPor(actor)
                .fechaCreacion(ahora)
                .build();
    }

    public void anular(String motivo, Integer actor, LocalDateTime ahora) {
        if (estado == EstadoMovimiento.ANULADO) {
            throw new ReglaNegocioExcepcion("El movimiento ya está anulado");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioExcepcion("Indique el motivo de la anulación");
        }
        estado = EstadoMovimiento.ANULADO;
        motivoAnulacion = motivo.trim();
        modificadoPor = actor;
        fechaModificacion = ahora;
    }

    public boolean esVenta() {
        return tipo.descuentaInventario();
    }

    /** Texto que se anota en el inventario para rastrear la venta. */
    public String referencia() {
        return PREFIJO_REFERENCIA + id + " (" + concepto + ")";
    }

    /** Monto con signo: positivo si ingresa, negativo si egresa. */
    public BigDecimal montoConSigno() {
        return tipo.naturaleza() == Naturaleza.INGRESO ? monto : monto.negate();
    }

    private static void validar(DatosMovimiento datos, LocalDate hoy) {
        if (datos.concepto() == null || datos.concepto().isBlank()) {
            throw new ReglaNegocioExcepcion("Indique el concepto del movimiento");
        }
        if (datos.monto() == null || datos.monto().signum() <= 0) {
            throw new ReglaNegocioExcepcion("El monto debe ser mayor a cero");
        }
        if (datos.fecha() == null || datos.fecha().isAfter(hoy)) {
            throw new ReglaNegocioExcepcion("La fecha del movimiento no puede ser futura");
        }
        if (datos.evento() != null && datos.proyecto() == null) {
            throw new ReglaNegocioExcepcion("Para imputar a un evento indique también su proyecto");
        }
        validarProveedor(datos);
        validarItems(datos);
    }

    private static void validarProveedor(DatosMovimiento datos) {
        TipoMovimiento tipo = datos.tipo();
        if (tipo.requiereProveedor() && datos.proveedor() == null) {
            throw new ReglaNegocioExcepcion("El tipo " + tipo.nombre() + " requiere indicar el proveedor");
        }
        if (tipo.naturaleza() == Naturaleza.INGRESO && datos.proveedor() != null) {
            throw new ReglaNegocioExcepcion("Los ingresos no se registran con proveedor");
        }
    }

    /** Solo las ventas llevan detalle, sin repetir unidades ni existencias. */
    private static void validarItems(DatosMovimiento datos) {
        List<ItemVenta> items = datos.items() == null ? List.of() : datos.items();
        if (!datos.tipo().descuentaInventario()) {
            if (!items.isEmpty()) {
                throw new ReglaNegocioExcepcion("Solo las ventas de equipos indican qué sale del inventario");
            }
            return;
        }
        if (items.isEmpty()) {
            throw new ReglaNegocioExcepcion("Indique los equipos o cantidades que se venden");
        }
        Set<String> claves = new HashSet<>();
        for (ItemVenta item : items) {
            if (item.cantidad() <= 0) {
                throw new ReglaNegocioExcepcion("La cantidad vendida debe ser mayor a cero");
            }
            String clave = item.esUnidad() ? "A" + item.idActivo() : "E" + item.idExistencia();
            if (!claves.add(clave)) {
                throw new ReglaNegocioExcepcion("Un equipo o lote está repetido en la venta: " + item.descripcion());
            }
        }
    }

    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
