package com.visual.backend.activos.aplicacion.dto;

import com.visual.backend.activos.dominio.modelo.TipoMovimientoLote;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** cantidad positiva; solo AJUSTE admite negativos. */
public record ComandoMovimientoLote(@NotNull Integer idModelo, @NotNull Integer idBodega,
                                    @NotNull TipoMovimientoLote tipo, @NotNull Integer cantidad,
                                    @Size(max = 255) String motivo) {
}
