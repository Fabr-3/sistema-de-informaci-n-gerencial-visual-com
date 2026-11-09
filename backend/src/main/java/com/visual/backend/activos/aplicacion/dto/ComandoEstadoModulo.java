package com.visual.backend.activos.aplicacion.dto;

import com.visual.backend.activos.dominio.modelo.EstadoModuloLed;

import jakarta.validation.constraints.NotNull;

public record ComandoEstadoModulo(@NotNull EstadoModuloLed estado) {
}
