package com.visual.backend.logistica.aplicacion.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Registro o edicion del personal tecnico; idUsuario nulo = sin cuenta vinculada. */
public record ComandoPersonal(@NotBlank @Size(max = 100) String nombres,
                              @NotBlank @Size(max = 100) String apellidos,
                              @Size(max = 30) String telefono,
                              @NotNull Integer idTipoDocumento,
                              @NotBlank @Size(max = 50) String numeroDocumento,
                              @Size(max = 10) List<@NotNull Integer> idsEspecialidades,
                              Integer idUsuario,
                              @NotNull Boolean activo) {
}
