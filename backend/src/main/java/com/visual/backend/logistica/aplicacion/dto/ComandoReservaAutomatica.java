package com.visual.backend.logistica.aplicacion.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Cantidades por modelo; el sistema elige las unidades libres. */
public record ComandoReservaAutomatica(@NotEmpty @Size(max = 50) List<@Valid @NotNull Pedido> pedidos) {

    public record Pedido(@NotNull Integer idModelo,
                         @NotNull @Positive @Max(ComandoReserva.MAXIMO_ACTIVOS) Integer cantidad) {
    }
}
