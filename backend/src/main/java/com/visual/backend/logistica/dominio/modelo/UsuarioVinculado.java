package com.visual.backend.logistica.dominio.modelo;

/**
 * Cuenta del sistema vinculada a una persona del personal tecnico, que le
 * permite consultar sus propias asignaciones.
 */
public record UsuarioVinculado(Integer id, String nombreUsuario, String nombreCompleto) {
}
