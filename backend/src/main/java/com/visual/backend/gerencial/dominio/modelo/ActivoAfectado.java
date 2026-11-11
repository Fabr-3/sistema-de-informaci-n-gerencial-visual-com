package com.visual.backend.gerencial.dominio.modelo;

/** Identificacion del activo al que se refiere una alerta. */
public record ActivoAfectado(Integer idActivo, String codigo, String modelo, String categoria) {
}
