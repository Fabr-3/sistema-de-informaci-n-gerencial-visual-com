package com.visual.backend.comercial.aplicacion.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ComandoCliente(@NotNull Integer idTipoCliente,
                             @NotBlank @Size(max = 150) String razonSocial,
                             @Size(max = 500) String direccion,
                             @NotEmpty @Size(max = 10) List<@Valid Documento> documentos,
                             @Size(max = 20) List<@Valid Contacto> contactos) {

    /** id nulo para documentos nuevos. */
    public record Documento(Integer id, @NotNull Integer idTipoDocumento, @NotBlank @Size(max = 50) String numero) {
    }

    /** id nulo para contactos nuevos. */
    public record Contacto(Integer id, @NotNull Integer idTipoContacto, @NotBlank @Size(max = 150) String valor,
                           @Size(max = 100) String referencia) {
    }
}
