package com.visual.backend.comercial.aplicacion.dto;

import com.visual.backend.comercial.dominio.modelo.EstadoEvento;

import jakarta.validation.constraints.NotNull;

/** Solo EJECUTADO o CANCELADO (un evento no vuelve a programado). */
public record ComandoEstadoEvento(@NotNull EstadoEvento estado) {
}
