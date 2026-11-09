package com.visual.backend.logistica.aplicacion.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

public record ComandoAsignacion(@NotNull Integer idPersonal, @NotNull Integer idRol, @NotNull LocalDateTime inicio,
                                @NotNull LocalDateTime fin) {
}
