package com.visual.backend.activos.aplicacion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ComandoTrasladoLote(@NotNull Integer idModelo, @NotNull Integer idBodegaOrigen,
                                  @NotNull Integer idBodegaDestino, @NotNull @Positive Integer cantidad,
                                  @Size(max = 255) String motivo) {
}
