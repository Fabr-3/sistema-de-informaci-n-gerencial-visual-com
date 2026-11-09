package com.visual.backend.accesos.aplicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComandoCambioContrasena(
        @NotBlank @Size(max = 72) String contrasenaActual,
        @NotBlank @Size(max = 72) String nuevaContrasena) {

    @Override
    public String toString() {
        return "ComandoCambioContrasena[***]";
    }
}
