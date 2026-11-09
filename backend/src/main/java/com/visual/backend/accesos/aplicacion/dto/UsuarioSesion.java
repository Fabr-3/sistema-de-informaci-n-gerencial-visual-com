package com.visual.backend.accesos.aplicacion.dto;

import java.util.List;

import com.visual.backend.accesos.dominio.modelo.Usuario;

/** Datos del usuario autenticado que necesita la interfaz. */
public record UsuarioSesion(Integer id, String nombreUsuario, String nombreCompleto, String email,
                            String codigoRol, String nombreRol, List<String> permisos,
                            boolean debeCambiarContrasena) {

    public static UsuarioSesion de(Usuario usuario) {
        return new UsuarioSesion(
                usuario.getId(),
                usuario.getNombreUsuario(),
                usuario.nombreCompleto(),
                usuario.getEmail(),
                usuario.getRol().getCodigo().name(),
                usuario.getRol().getNombre(),
                usuario.permisosEfectivos().stream().sorted().toList(),
                usuario.isDebeCambiarContrasena());
    }
}
