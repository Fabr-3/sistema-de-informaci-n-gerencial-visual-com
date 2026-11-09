package com.visual.backend.activos.aplicacion.dto;

import java.util.List;

import com.visual.backend.activos.dominio.modelo.TipoControl;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ComandoModelo(@NotNull Integer idFabricante, @NotNull Integer idCategoria,
                            @NotBlank @Size(max = 100) String nombre, @NotNull TipoControl tipoControl,
                            @Positive Integer horasMantenimiento,
                            @Pattern(regexp = "^[A-Za-z0-9]{2,10}$") String prefijoCodigo,
                            @Valid @Size(max = 30) List<ComandoEspecificacion> especificaciones) {
}
