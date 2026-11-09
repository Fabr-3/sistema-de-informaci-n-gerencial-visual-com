package com.visual.backend.accesos.aplicacion.dto;

import jakarta.validation.constraints.NotNull;

public record ComandoCambioEstado(@NotNull Boolean activo) {
}
