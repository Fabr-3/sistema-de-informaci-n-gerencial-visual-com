package com.visual.backend.logistica.aplicacion.dto;

import java.time.LocalDateTime;

import com.visual.backend.logistica.dominio.modelo.TipoJornada;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ComandoJornada(@NotNull TipoJornada tipo, @NotNull LocalDateTime inicio, @NotNull LocalDateTime fin,
                             @Size(max = 500) String descripcion) {
}
