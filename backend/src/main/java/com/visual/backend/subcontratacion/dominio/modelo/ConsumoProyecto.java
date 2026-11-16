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
 * Consumible gastado en un proyecto o evento. Copia el costo unitario vigente
 * al registrarse: cambios posteriores del costo no alteran el margen ya
 * calculado. No genera movimiento financiero (la compra ya es un egreso).
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConsumoProyecto {

    private Integer id;
    private ElementoCatalogo consumible;
    private String unidadMedida;
    private ElementoCatalogo proyecto;
    private ElementoCatalogo evento;
    private BigDecimal cantidad;
    private BigDecimal costoUnitario;
    private LocalDate fecha;
    private EstadoConsumo estado;
    private String motivoAnulacion;
    private Integer registradoPor;
    private String nombreRegistrador;
    private LocalDateTime fechaCreacion;
    private Integer modificadoPor;
    private LocalDateTime fechaModificacion;

    public static ConsumoProyecto registrar(Consumible consumible, ElementoCatalogo proyecto, ElementoCatalogo evento,
                                            BigDecimal cantidad, LocalDate fecha, Integer actor,
                                            LocalDateTime ahora) {
        if (fecha == null || fecha.isAfter(ahora.toLocalDate())) {
            throw new ReglaNegocioExcepcion("La fecha del consumo no puede ser futura");
        }
        return ConsumoProyecto.builder()
                .consumible(new ElementoCatalogo(consumible.getId(), consumible.getNombre()))
                .unidadMedida(consumible.getUnidadMedida())
                .proyecto(proyecto)
                .evento(evento)
                .cantidad(cantidad)
                .costoUnitario(consumible.getCostoUnitario())
                .fecha(fecha)
                .estado(EstadoConsumo.REGISTRADO)
                .registradoPor(actor)
                .fechaCreacion(ahora)
                .build();
    }

    public BigDecimal costoTotal() {
        return cantidad.multiply(costoUnitario).setScale(Consumible.DECIMALES, RoundingMode.HALF_UP);
    }

    /** Texto que se anota en el movimiento de stock. */
    public String referencia() {
        return "Consumo · " + (evento == null ? proyecto.nombre() : evento.nombre());
    }

    public void anular(String motivo, Integer actor, LocalDateTime ahora) {
        if (estado == EstadoConsumo.ANULADO) {
            throw new ReglaNegocioExcepcion("El consumo ya está anulado");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioExcepcion("Indique el motivo de la anulación");
        }
        estado = EstadoConsumo.ANULADO;
        motivoAnulacion = motivo.trim();
        modificadoPor = actor;
        fechaModificacion = ahora;
    }
}
