package com.visual.backend.finanzas.aplicacion.dto;

import jakarta.validation.constraints.NotNull;

public record ComandoEstadoProveedor(@NotNull Boolean activo) {
}
