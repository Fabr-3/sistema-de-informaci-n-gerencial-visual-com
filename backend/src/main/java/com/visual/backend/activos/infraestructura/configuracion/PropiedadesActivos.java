package com.visual.backend.activos.infraestructura.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Configuracion del modulo de activos (prefijo app.activos). */
@Validated
@ConfigurationProperties(prefix = "app.activos")
public record PropiedadesActivos(@Min(1) @Max(100) int porcentajeAlertaMantenimiento) {
}
