package com.visual.backend.subcontratacion.dominio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Servicio contratado a un proveedor para un proyecto (y opcionalmente uno de
 * sus eventos). Su costo se registra como egreso financiero vinculado; no se
 * edita: un error se corrige anulando, lo que anula tambien el egreso.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Subcontratacion {

    public static final int DECIMALES = 2;
    static final int LONGITUD_MAXIMA_DESCRIPCION = 2000;

    private Integer id;
    private ElementoCatalogo proveedor;
    private ElementoCatalogo servicio;
    private ElementoCatalogo proyecto;
    private ElementoCatalogo evento;
    /** Modelo y cantidad subalquilados; nulos en una subcontratacion de servicios. */
    private ElementoCatalogo modelo;
    private Integer cantidad;
    private String descripcion;
    private BigDecimal costo;
    private LocalDate fecha;
    private EstadoSubcontratacion estado;
    private String motivoAnulacion;
    private Integer idMovimiento;
    private Integer creadoPor;
    private LocalDateTime fechaCreacion;
    private Integer modificadoPor;
    private LocalDateTime fechaModificacion;

    public static Subcontratacion registrar(DatosSubcontratacion datos, Integer actor, LocalDateTime ahora) {
        validar(datos, ahora.toLocalDate());
        return Subcontratacion.builder()
                .proveedor(datos.proveedor())
                .servicio(datos.servicio())
                .proyecto(datos.proyecto())
                .evento(datos.evento())
                .modelo(datos.modelo())
                .cantidad(datos.modelo() == null ? null : datos.cantidad())
                .descripcion(datos.descripcion().trim())
                .costo(datos.costo().setScale(DECIMALES, RoundingMode.HALF_UP))
                .fecha(datos.fecha())
                .estado(EstadoSubcontratacion.REGISTRADA)
                .creadoPor(actor)
                .fechaCreacion(ahora)
                .build();
    }

    public boolean esSubalquiler() {
        return modelo != null;
    }

    /** Concepto del egreso financiero que genera. */
    public String concepto() {
        if (esSubalquiler()) {
            return "Subalquiler · " + proveedor.nombre() + " · " + cantidad + " × " + modelo.nombre();
        }
        String detalle = servicio == null ? descripcion : servicio.nombre();
        return "Subcontratación · " + proveedor.nombre() + " · " + detalle;
    }

    public void vincularEgreso(Integer idEgreso) {
        idMovimiento = idEgreso;
    }

    public void anular(String motivo, Integer actor, LocalDateTime ahora) {
        if (estado == EstadoSubcontratacion.ANULADA) {
            throw new ReglaNegocioExcepcion("La subcontratación ya está anulada");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioExcepcion("Indique el motivo de la anulación");
        }
        estado = EstadoSubcontratacion.ANULADA;
        motivoAnulacion = motivo.trim();
        modificadoPor = actor;
        fechaModificacion = ahora;
    }

    private static void validar(DatosSubcontratacion datos, LocalDate hoy) {
        if (datos.descripcion() == null || datos.descripcion().isBlank()) {
            throw new ReglaNegocioExcepcion("Describa el servicio subcontratado");
        }
        if (datos.descripcion().trim().length() > LONGITUD_MAXIMA_DESCRIPCION) {
            throw new ReglaNegocioExcepcion("La descripción no puede superar " + LONGITUD_MAXIMA_DESCRIPCION
                    + " caracteres");
        }
        if (datos.costo() == null || datos.costo().signum() <= 0) {
            throw new ReglaNegocioExcepcion("El costo debe ser mayor a cero");
        }
        if (datos.modelo() != null) {
            if (datos.cantidad() == null || datos.cantidad() <= 0) {
                throw new ReglaNegocioExcepcion("Indique cuántas unidades se subalquilan");
            }
            if (datos.evento() == null) {
                throw new ReglaNegocioExcepcion("Un subalquiler de equipos se registra para un evento");
            }
        }
        if (datos.fecha() == null || datos.fecha().isAfter(hoy)) {
            throw new ReglaNegocioExcepcion("La fecha de la subcontratación no puede ser futura");
        }
    }
}
