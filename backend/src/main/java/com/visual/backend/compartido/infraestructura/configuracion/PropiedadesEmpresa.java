package com.visual.backend.compartido.infraestructura.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/** Datos de la empresa impresos en los documentos y reportes (prefijo app.empresa). */
@Validated
@ConfigurationProperties(prefix = "app.empresa")
public record PropiedadesEmpresa(@NotBlank String razonSocial, String direccion, @NotBlank String ciudad,
                                 String email, String telefono, String nit) {
}
