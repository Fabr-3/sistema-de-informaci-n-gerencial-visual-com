package com.visual.backend.gerencial.aplicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComandoAtencionAlerta(@NotBlank @Size(max = 1000) String comentario) {
}
