package com.visual.backend.comercial.aplicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Motivo de una decision (p. ej. el rechazo de una cotizacion). */
public record ComandoMotivo(@NotBlank @Size(max = 1000) String motivo) {
}
