package com.visual.backend.activos.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** Mantenimiento de un activo: se inicia y luego se finaliza con su resultado. */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Mantenimiento {

    private Integer id;
    private Integer idActivo;
    private TipoMantenimiento tipo;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String descripcion;
    private String observaciones;
    private CondicionFinal condicionFinal;
    private BigDecimal costo;
    private EstadoMantenimiento estado;
    private Integer creadoPor;
    private Integer modificadoPor;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;

    public static Mantenimiento iniciar(Integer idActivo, TipoMantenimiento tipo, LocalDateTime fechaInicio,
                                        String descripcion, Integer actor, LocalDateTime ahora) {
        if (fechaInicio == null || fechaInicio.isAfter(ahora)) {
            throw new ReglaNegocioExcepcion("La fecha de inicio del mantenimiento no puede ser futura");
        }
        return Mantenimiento.builder()
                .idActivo(idActivo)
                .tipo(tipo)
                .fechaInicio(fechaInicio)
                .descripcion(descripcion == null || descripcion.isBlank() ? null : descripcion.trim())
                .estado(EstadoMantenimiento.EN_PROCESO)
                .creadoPor(actor)
                .fechaCreacion(ahora)
                .build();
    }

    public void finalizar(LocalDateTime fin, CondicionFinal condicion, String textoObservaciones,
                          BigDecimal costoMantenimiento, Integer actor, LocalDateTime ahora) {
        if (estado == EstadoMantenimiento.FINALIZADO) {
            throw new ReglaNegocioExcepcion("El mantenimiento ya fue finalizado");
        }
        if (fin == null || fin.isBefore(fechaInicio) || fin.isAfter(ahora)) {
            throw new ReglaNegocioExcepcion(
                    "La fecha de finalización debe estar entre el inicio del mantenimiento y la fecha actual");
        }
        if (condicion == null) {
            throw new ReglaNegocioExcepcion("Indique la condición final del activo");
        }
        fechaFin = fin;
        condicionFinal = condicion;
        observaciones = textoObservaciones == null || textoObservaciones.isBlank() ? null : textoObservaciones.trim();
        costo = costoMantenimiento;
        estado = EstadoMantenimiento.FINALIZADO;
        modificadoPor = actor;
        fechaModificacion = ahora;
    }

    public boolean estaEnProceso() {
        return estado == EstadoMantenimiento.EN_PROCESO;
    }
}
