package com.visual.backend.comercial.aplicacion.dto;

import com.visual.backend.comercial.dominio.modelo.EstadoProyecto;

import jakarta.validation.constraints.NotNull;

public record ComandoEstadoProyecto(@NotNull EstadoProyecto estado) {
}
