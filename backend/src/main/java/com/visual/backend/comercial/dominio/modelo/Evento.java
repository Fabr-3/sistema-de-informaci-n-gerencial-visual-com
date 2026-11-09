package com.visual.backend.comercial.dominio.modelo;

import java.time.LocalDateTime;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Evento del cronograma de un proyecto. Debe quedar dentro del periodo del
 * proyecto (regla tambien protegida por un trigger). Se modifica mientras
 * esta programado; luego se marca ejecutado o cancelado.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Evento {

    private Integer id;
    private Integer idProyecto;
    private Lugar lugar;
    private String nombre;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private EstadoEvento estado;
    private Integer creadoPor;
    private LocalDateTime fechaCreacion;
    private Integer modificadoPor;
    private LocalDateTime fechaModificacion;

    public static Evento nuevo(Proyecto proyecto, DatosEvento datos, Integer actor, LocalDateTime ahora) {
        proyecto.exigirVigente("programar eventos");
        validar(proyecto, datos);
        return Evento.builder()
                .idProyecto(proyecto.getId())
                .lugar(datos.lugar())
                .nombre(datos.nombre().trim())
                .fechaInicio(datos.fechaInicio())
                .fechaFin(datos.fechaFin())
                .estado(EstadoEvento.PROGRAMADO)
                .creadoPor(actor)
                .fechaCreacion(ahora)
                .build();
    }

    public void actualizar(Proyecto proyecto, DatosEvento datos, Integer actor, LocalDateTime ahora) {
        exigirProgramado("modificar");
        validar(proyecto, datos);
        lugar = datos.lugar();
        nombre = datos.nombre().trim();
        fechaInicio = datos.fechaInicio();
        fechaFin = datos.fechaFin();
        registrarModificacion(actor, ahora);
    }

    /** Un evento se marca ejecutado una vez iniciado. */
    public void marcarEjecutado(Integer actor, LocalDateTime ahora) {
        exigirProgramado("marcar como ejecutado");
        if (fechaInicio.isAfter(ahora)) {
            throw new ReglaNegocioExcepcion("El evento " + nombre + " aún no ha comenzado");
        }
        estado = EstadoEvento.EJECUTADO;
        registrarModificacion(actor, ahora);
    }

    public void cancelar(Integer actor, LocalDateTime ahora) {
        exigirProgramado("cancelar");
        estado = EstadoEvento.CANCELADO;
        registrarModificacion(actor, ahora);
    }

    private void exigirProgramado(String accion) {
        if (estado != EstadoEvento.PROGRAMADO) {
            throw new ReglaNegocioExcepcion("No se puede " + accion + " un evento " + estado.name().toLowerCase());
        }
    }

    private void registrarModificacion(Integer actor, LocalDateTime ahora) {
        modificadoPor = actor;
        fechaModificacion = ahora;
    }

    private static void validar(Proyecto proyecto, DatosEvento datos) {
        if (datos.nombre() == null || datos.nombre().isBlank()) {
            throw new ReglaNegocioExcepcion("Indique el nombre del evento");
        }
        if (!datos.fechaFin().isAfter(datos.fechaInicio())) {
            throw new ReglaNegocioExcepcion("El fin del evento debe ser posterior a su inicio");
        }
        if (!proyecto.contiene(datos.fechaInicio(), datos.fechaFin())) {
            throw new ReglaNegocioExcepcion("El evento debe realizarse dentro del periodo del proyecto ("
                    + proyecto.getFechaInicio() + " a " + proyecto.getFechaFin() + ")");
        }
    }
}
