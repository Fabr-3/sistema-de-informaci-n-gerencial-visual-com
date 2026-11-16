package com.visual.backend.gerencial.dominio.modelo;

import java.time.LocalDateTime;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Alerta registrada, con identidad y estado. Nace pendiente al detectarse la
 * situacion; el gerente la marca como atendida (no se reabre mientras la
 * situacion persista) y pasa a resuelta cuando la situacion desaparece.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Alerta {

    static final int LONGITUD_MAXIMA_COMENTARIO = 1000;

    private Long id;
    private TipoAlerta tipo;
    private Severidad severidad;
    private Integer idActivo;
    private Integer idEvento;
    private String clave;
    private String codigo;
    private String modelo;
    private String categoria;
    private String detalle;
    private LocalDateTime fechaGeneracion;
    private EstadoAlerta estado;
    private Integer atendidaPor;
    private String nombreAtendio;
    private LocalDateTime fechaAtencion;
    private String comentarioAtencion;
    private LocalDateTime fechaResolucion;

    public static Alerta generar(AlertaActivo calculada, LocalDateTime ahora) {
        return Alerta.builder()
                .tipo(calculada.tipo())
                .severidad(calculada.severidad())
                .idActivo(calculada.idActivo())
                .idEvento(calculada.idEvento())
                .clave(calculada.clave())
                .codigo(calculada.codigo())
                .modelo(calculada.modelo())
                .categoria(calculada.categoria())
                .detalle(calculada.detalle())
                .fechaGeneracion(ahora)
                .estado(EstadoAlerta.PENDIENTE)
                .build();
    }

    /** La misma situacion sigue presente: se actualiza su detalle (dias, horas, stock) sin cambiar el estado. */
    public boolean actualizar(AlertaActivo calculada) {
        boolean cambio = !calculada.detalle().equals(detalle);
        detalle = calculada.detalle();
        return cambio;
    }

    public void atender(String comentario, Integer actor, LocalDateTime ahora) {
        if (estado != EstadoAlerta.PENDIENTE) {
            throw new ReglaNegocioExcepcion("Solo se atienden alertas pendientes; esta ya está "
                    + estado.name().toLowerCase());
        }
        if (comentario == null || comentario.isBlank()) {
            throw new ReglaNegocioExcepcion("Indique qué se hizo para atender la alerta");
        }
        String limpio = comentario.trim();
        if (limpio.length() > LONGITUD_MAXIMA_COMENTARIO) {
            throw new ReglaNegocioExcepcion("El comentario no puede superar " + LONGITUD_MAXIMA_COMENTARIO
                    + " caracteres");
        }
        estado = EstadoAlerta.ATENDIDA;
        comentarioAtencion = limpio;
        atendidaPor = actor;
        fechaAtencion = ahora;
    }

    public void resolver(LocalDateTime ahora) {
        estado = EstadoAlerta.RESUELTA;
        fechaResolucion = ahora;
    }

    /** Identifica la situacion: una sola alerta abierta por tipo y clave. */
    public String identidad() {
        return identidad(tipo, clave);
    }

    public static String identidad(TipoAlerta tipo, String clave) {
        return tipo.name() + ":" + clave;
    }

    public AlertaActivo comoCalculada() {
        return new AlertaActivo(tipo, severidad, idActivo, idEvento, clave, codigo, modelo, categoria, detalle);
    }
}
