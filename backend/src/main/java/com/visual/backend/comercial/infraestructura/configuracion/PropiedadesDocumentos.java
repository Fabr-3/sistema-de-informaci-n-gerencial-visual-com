package com.visual.backend.comercial.infraestructura.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/** Carpeta de los contratos firmados (prefijo app.comercial). */
@Validated
@ConfigurationProperties(prefix = "app.comercial")
public record PropiedadesDocumentos(@NotBlank String directorioContratos) {
}
