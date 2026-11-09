package com.visual.backend.accesos.infraestructura.seguridad;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Configuracion de seguridad (prefijo app.seguridad). Se valida al arrancar. */
@Validated
@ConfigurationProperties(prefix = "app.seguridad")
public record PropiedadesSeguridad(@Valid @NotNull Jwt jwt, @Valid @NotNull Refresco refresco,
                                   @Valid @NotNull Bloqueo bloqueo,
                                   @Valid @NotNull AdministradorInicial administradorInicial) {

    /** HS256 exige una clave de al menos 256 bits (32 bytes). */
    public record Jwt(@NotBlank @Size(min = 32) String secreto, @NotBlank String emisor,
                      @NotNull Duration duracionAcceso) {

        @Override
        public String toString() {
            return "Jwt[emisor=" + emisor + ", duracionAcceso=" + duracionAcceso + "]";
        }
    }

    public record Refresco(@NotNull Duration duracion, @NotBlank String nombreCookie, boolean cookieSegura) {
    }

    public record Bloqueo(@Min(1) int intentosMaximos, @NotNull Duration duracion) {
    }

    public record AdministradorInicial(@NotBlank String nombreUsuario, @NotBlank String nombres,
                                       @NotBlank String apellidoPaterno, @NotBlank String email,
                                       @NotBlank String contrasena, boolean debeCambiarContrasena) {

        @Override
        public String toString() {
            return "AdministradorInicial[nombreUsuario=" + nombreUsuario + "]";
        }
    }
}
