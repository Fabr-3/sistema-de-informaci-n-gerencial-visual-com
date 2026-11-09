package com.visual.backend.comercial.aplicacion.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ComandoEvento(@NotNull Integer idLugar, @NotBlank @Size(max = 200) String nombre,
                            @NotNull LocalDateTime fechaInicio, @NotNull LocalDateTime fechaFin) {
}
