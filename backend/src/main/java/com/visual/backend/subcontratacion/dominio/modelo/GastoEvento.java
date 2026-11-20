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
 * Gasto que el personal tecnico hace durante un evento y que no esta en el
 * catalogo de consumibles (un repuesto, un pasaje, un material de ultimo
 * momento). Lleva comprobante y genera un egreso imputado al proyecto y al
 * evento; no mueve stock. No se edita: un error se corrige anulandolo, lo
 * que anula tambien el egreso.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GastoEvento {

    public static final int DECIMALES = 2;
    static final int LONGITUD_MAXIMA_DESCRIPCION = 500;

    private Integer id;
    private ElementoCatalogo proyecto;
    private ElementoCatalogo evento;
    private String descripcion;
    private BigDecimal monto;
    private LocalDate fecha;
    private Comprobante comprobante;
    private EstadoConsumo estado;
    private String motivoAnulacion;
    private Integer idMovimiento;
    private Integer registradoPor;
    private String nombreRegistrador;
    private LocalDateTime fechaCreacion;
    private Integer modificadoPor;
    private LocalDateTime fechaModificacion;

    public static GastoEvento registrar(DatosGasto datos, Integer actor, LocalDateTime ahora) {
        validar(datos, ahora.toLocalDate());
        return GastoEvento.builder()
                .proyecto(datos.proyecto())
                .evento(datos.evento())
                .descripcion(datos.descripcion().trim())
                .monto(datos.monto().setScale(DECIMALES, RoundingMode.HALF_UP))
                .fecha(datos.fecha())
                .comprobante(datos.comprobante())
                .estado(EstadoConsumo.REGISTRADO)
                .registradoPor(actor)
                .fechaCreacion(ahora)
                .build();
    }

    /** Concepto del egreso financiero que genera. */
    public String concepto() {
        return "Gasto del evento · " + evento.nombre() + " · " + descripcion;
    }

    public void vincularEgreso(Integer idEgreso) {
        idMovimiento = idEgreso;
    }

    public void anular(String motivo, Integer actor, LocalDateTime ahora) {
        if (estado == EstadoConsumo.ANULADO) {
            throw new ReglaNegocioExcepcion("El gasto ya está anulado");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioExcepcion("Indique el motivo de la anulación");
        }
        estado = EstadoConsumo.ANULADO;
        motivoAnulacion = motivo.trim();
        modificadoPor = actor;
        fechaModificacion = ahora;
    }

    private static void validar(DatosGasto datos, LocalDate hoy) {
        if (datos.evento() == null) {
            throw new ReglaNegocioExcepcion("Indique el evento donde se hizo el gasto");
        }
        if (datos.descripcion() == null || datos.descripcion().isBlank()) {
            throw new ReglaNegocioExcepcion("Describa el gasto (qué se compró o pagó)");
        }
        if (datos.descripcion().trim().length() > LONGITUD_MAXIMA_DESCRIPCION) {
            throw new ReglaNegocioExcepcion("La descripción no puede superar " + LONGITUD_MAXIMA_DESCRIPCION
                    + " caracteres");
        }
        if (datos.monto() == null || datos.monto().signum() <= 0) {
            throw new ReglaNegocioExcepcion("El monto debe ser mayor a cero");
        }
        if (datos.fecha() == null || datos.fecha().isAfter(hoy)) {
            throw new ReglaNegocioExcepcion("La fecha del gasto no puede ser futura");
        }
        if (datos.comprobante() == null) {
            throw new ReglaNegocioExcepcion("Adjunte el comprobante del gasto (foto o PDF)");
        }
    }
}
