package com.visual.backend.logistica.aplicacion.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Equipos a reservar para el evento: se reservan todos o ninguno. */
public record ComandoReserva(@NotEmpty @Size(max = ComandoReserva.MAXIMO_ACTIVOS) List<@NotNull Integer> idsActivos) {

    public static final int MAXIMO_ACTIVOS = 100;
}
