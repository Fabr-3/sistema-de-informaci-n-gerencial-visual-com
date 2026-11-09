package com.visual.backend.accesos.aplicacion.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ComandoCrearUsuario(
        @NotBlank @Size(min = 3, max = 50) String nombreUsuario,
        @NotBlank @Size(max = 100) String nombres,
        @NotBlank @Size(max = 100) String apellidoPaterno,
        @Size(max = 100) String apellidoMaterno,
        @NotBlank @Email @Size(max = 150) String email,
        @NotNull Integer idRol,
        LocalDateTime fechaExpiracion,
        @NotBlank @Size(max = 72) String contrasenaTemporal) {

    @Override
    public String toString() {
        return "ComandoCrearUsuario[nombreUsuario=" + nombreUsuario + ", idRol=" + idRol + "]";
    }
}
