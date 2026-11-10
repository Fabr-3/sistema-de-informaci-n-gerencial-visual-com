package com.visual.backend.finanzas.aplicacion.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ComandoProveedor(@NotBlank @Size(max = 150) String razonSocial,
                               @Pattern(regexp = "^$|\\d{5,15}", message = "debe tener entre 5 y 15 dígitos")
                               String nit,
                               @Size(max = 30) String telefono,
                               @Email @Size(max = 150) String email,
                               @Size(max = 20) List<@Size(max = 150) String> servicios) {
}
