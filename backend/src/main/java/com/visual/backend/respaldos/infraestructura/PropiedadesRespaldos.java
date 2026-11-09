package com.visual.backend.respaldos.infraestructura;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Configuracion de respaldos (prefijo app.respaldos). */
@Validated
@ConfigurationProperties(prefix = "app.respaldos")
public record PropiedadesRespaldos(@NotBlank String directorio, @NotBlank String rutaPgDump, boolean programado,
                                   @NotBlank String cron, @NotNull Duration tiempoMaximo) {
}
