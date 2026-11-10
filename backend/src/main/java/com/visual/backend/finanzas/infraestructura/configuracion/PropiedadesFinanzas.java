package com.visual.backend.finanzas.infraestructura.configuracion;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Configuracion del modulo financiero (prefijo app.finanzas). */
@Validated
@ConfigurationProperties(prefix = "app.finanzas")
public record PropiedadesFinanzas(@NotNull @Positive BigDecimal umbralMovimientoSignificativo,
                                  @Min(1) int diasMovimientosRecientes) {
}
