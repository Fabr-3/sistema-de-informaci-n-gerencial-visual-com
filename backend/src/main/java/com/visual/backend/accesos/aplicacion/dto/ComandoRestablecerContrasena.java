package com.visual.backend.accesos.aplicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComandoRestablecerContrasena(@NotBlank @Size(max = 72) String contrasenaTemporal) {

    @Override
    public String toString() {
        return "ComandoRestablecerContrasena[***]";
    }
}
