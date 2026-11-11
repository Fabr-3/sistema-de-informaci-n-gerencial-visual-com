package com.visual.backend.gerencial.infraestructura.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

/** Configuracion del modulo de informacion gerencial (prefijo app.gerencial). */
@Validated
@ConfigurationProperties(prefix = "app.gerencial")
public record PropiedadesGerencial(@Min(1) int diasMantenimientoProlongado, @Min(1) int diasSinUso,
                                   @Min(2) int correctivosFallasRecurrentes,
                                   @Min(2) int proyectosClienteRecurrente) {
}
