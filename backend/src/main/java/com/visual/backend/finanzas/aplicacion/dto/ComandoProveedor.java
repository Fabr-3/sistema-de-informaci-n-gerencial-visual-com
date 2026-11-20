package com.visual.backend.finanzas.aplicacion.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Mismos datos que un cliente: tipo, nombre o razon social, direccion y
 * documentos (opcionales). Sin tipo se registra como empresa privada.
 */
public record ComandoProveedor(Integer idTipoProveedor,
                               @NotBlank @Size(max = 150) String razonSocial,
                               @Size(max = 500) String direccion,
                               @Size(max = 10) List<@Valid Documento> documentos,
                               @Size(max = 30) String telefono,
                               @Email @Size(max = 150) String email,
                               @Size(max = 20) List<@Size(max = 150) String> servicios) {

    /** id nulo para documentos nuevos; complemento solo para el carnet de identidad. */
    public record Documento(Integer id, @NotNull Integer idTipoDocumento, @NotBlank @Size(max = 50) String numero,
                            @Size(max = 2) String complemento) {
    }
}
