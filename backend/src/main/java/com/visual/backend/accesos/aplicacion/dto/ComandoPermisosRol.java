package com.visual.backend.accesos.aplicacion.dto;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

public record ComandoPermisosRol(@NotNull Set<@NotNull Integer> idsPermisos) {
}
