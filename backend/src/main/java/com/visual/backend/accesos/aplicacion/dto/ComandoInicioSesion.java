package com.visual.backend.accesos.aplicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComandoInicioSesion(
        @NotBlank @Size(max = 50) String nombreUsuario,
        @NotBlank @Size(max = 72) String contrasena) {

    /** Evita exponer la contrasena si el comando llega a registrarse en logs. */
    @Override
    public String toString() {
        return "ComandoInicioSesion[nombreUsuario=" + nombreUsuario + "]";
    }
}
