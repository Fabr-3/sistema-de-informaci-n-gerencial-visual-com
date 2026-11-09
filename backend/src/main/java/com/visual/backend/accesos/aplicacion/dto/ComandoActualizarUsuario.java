package com.visual.backend.accesos.aplicacion.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** El nombre de usuario no se modifica: identifica la cuenta en la auditoria. */
public record ComandoActualizarUsuario(
        @NotBlank @Size(max = 100) String nombres,
        @NotBlank @Size(max = 100) String apellidoPaterno,
        @Size(max = 100) String apellidoMaterno,
        @NotBlank @Email @Size(max = 150) String email,
        @NotNull Integer idRol,
        LocalDateTime fechaExpiracion) {
}
