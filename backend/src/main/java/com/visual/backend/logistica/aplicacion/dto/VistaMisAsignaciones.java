package com.visual.backend.logistica.aplicacion.dto;

import java.util.List;

/** Asignaciones propias; vinculado = false si el usuario no corresponde a ningun personal tecnico. */
public record VistaMisAsignaciones(boolean vinculado, String nombre, List<VistaAsignacionPropia> asignaciones) {
}
