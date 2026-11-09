package com.visual.backend.comercial.dominio.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Proyecto generado a partir de una cotizacion aceptada. Agrupa los eventos
 * del cronograma y los contratos. El builder se reserva para la persistencia.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Proyecto {

    private Integer id;
    private Integer idCotizacion;
    private String numeroCotizacion;
    private ResumenCliente cliente;
    private String nombre;
    private String descripcion;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private EstadoProyecto estado;
    private Integer creadoPor;
    private LocalDateTime fechaCreacion;
    private Integer modificadoPor;
    private LocalDateTime fechaModificacion;

    /** El proyecto hereda el titulo y el periodo del evento de la cotizacion aceptada. */
    public static Proyecto desdeCotizacion(Cotizacion cotizacion, Integer actor, LocalDateTime ahora) {
        if (cotizacion.getEstado() != EstadoCotizacion.ACEPTADA) {
            throw new ReglaNegocioExcepcion("Solo una cotización aceptada genera un proyecto");
        }
        return Proyecto.builder()
                .idCotizacion(cotizacion.getId())
                .numeroCotizacion(cotizacion.getNumero())
                .cliente(cotizacion.getCliente())
                .nombre(cotizacion.getTitulo())
                .fechaInicio(cotizacion.getFechaEventoInicio())
                .fechaFin(cotizacion.getFechaEventoFin())
                .estado(EstadoProyecto.PLANIFICACION)
                .creadoPor(actor)
                .fechaCreacion(ahora)
                .build();
    }

    public void actualizar(String nuevoNombre, String nuevaDescripcion, LocalDate inicio, LocalDate fin,
                           Integer actor, LocalDateTime ahora) {
        exigirVigente("modificar");
        if (nuevoNombre == null || nuevoNombre.isBlank()) {
            throw new ReglaNegocioExcepcion("Indique el nombre del proyecto");
        }
        if (inicio != null && fin != null && fin.isBefore(inicio)) {
            throw new ReglaNegocioExcepcion("La fecha de fin del proyecto no puede ser anterior a la de inicio");
        }
        nombre = nuevoNombre.trim();
        descripcion = nuevaDescripcion == null || nuevaDescripcion.isBlank() ? null : nuevaDescripcion.trim();
        fechaInicio = inicio;
        fechaFin = fin;
        registrarModificacion(actor, ahora);
    }

    public void cambiarEstado(EstadoProyecto destino, Integer actor, LocalDateTime ahora) {
        if (!estado.puedeCambiarA(destino)) {
            throw new ReglaNegocioExcepcion("Un proyecto en " + estado + " no puede pasar a " + destino);
        }
        estado = destino;
        registrarModificacion(actor, ahora);
    }

    /** Un periodo esta dentro del proyecto si respeta sus fechas (cuando estan definidas). */
    public boolean contiene(LocalDateTime inicio, LocalDateTime fin) {
        return (fechaInicio == null || !inicio.toLocalDate().isBefore(fechaInicio))
                && (fechaFin == null || !fin.toLocalDate().isAfter(fechaFin));
    }

    public void exigirVigente(String accion) {
        if (!estado.estaVigente()) {
            throw new ReglaNegocioExcepcion("No se puede " + accion + " en un proyecto " + estado.name().toLowerCase());
        }
    }

    private void registrarModificacion(Integer actor, LocalDateTime ahora) {
        modificadoPor = actor;
        fechaModificacion = ahora;
    }
}
