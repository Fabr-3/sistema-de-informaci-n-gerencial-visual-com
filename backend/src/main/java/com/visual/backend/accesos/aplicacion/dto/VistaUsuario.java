package com.visual.backend.accesos.aplicacion.dto;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import com.visual.backend.accesos.dominio.modelo.Usuario;

/** Representacion publica de un usuario (nunca incluye el hash de la contrasena). */
public record VistaUsuario(
        Integer id,
        String nombreUsuario,
        String nombres,
        String apellidoPaterno,
        String apellidoMaterno,
        String nombreCompleto,
        String email,
        Integer idRol,
        String codigoRol,
        String nombreRol,
        boolean activo,
        boolean bloqueado,
        boolean expirado,
        LocalDateTime fechaExpiracion,
        boolean debeCambiarContrasena,
        LocalDateTime fechaUltimoAcceso,
        LocalDateTime fechaCreacion) {

    public static VistaUsuario de(Usuario usuario, LocalDateTime ahora) {
        return new VistaUsuario(
                usuario.getId(),
                usuario.getNombreUsuario(),
                usuario.getNombres(),
                usuario.getApellidoPaterno(),
                usuario.getApellidoMaterno(),
                usuario.nombreCompleto(),
                usuario.getEmail(),
                usuario.getRol().getId(),
                usuario.getRol().getCodigo().name(),
                usuario.getRol().getNombre(),
                usuario.isActivo(),
                usuario.estaBloqueado(ahora),
                usuario.estaExpirado(ahora),
                usuario.getFechaExpiracion(),
                usuario.isDebeCambiarContrasena(),
                usuario.getFechaUltimoAcceso(),
                usuario.getFechaCreacion());
    }

    /** Instantanea de los datos persistentes relevantes para la auditoria. */
    public Map<String, Object> comoMapaAuditoria() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("nombreUsuario", nombreUsuario);
        mapa.put("nombres", nombres);
        mapa.put("apellidoPaterno", apellidoPaterno);
        mapa.put("apellidoMaterno", apellidoMaterno);
        mapa.put("email", email);
        mapa.put("rol", codigoRol);
        mapa.put("activo", activo);
        mapa.put("fechaExpiracion", fechaExpiracion == null ? null : fechaExpiracion.toString());
        mapa.put("debeCambiarContrasena", debeCambiarContrasena);
        return mapa;
    }
}
