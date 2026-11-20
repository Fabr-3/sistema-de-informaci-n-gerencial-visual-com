package com.visual.backend.subcontratacion.infraestructura.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/** Carpeta donde se guardan los comprobantes de los gastos del evento. */
@Validated
@ConfigurationProperties(prefix = "app.subcontratacion")
public record PropiedadesComprobantes(@NotBlank String directorioComprobantes) {
}
