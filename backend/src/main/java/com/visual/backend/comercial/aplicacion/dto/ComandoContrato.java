package com.visual.backend.comercial.aplicacion.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/** Los terminos son clausulas particulares que se agregan a la plantilla estandar. */
public record ComandoContrato(@NotNull @PastOrPresent LocalDate fechaFirma, @Size(max = 10_000) String terminos) {
}
