package com.visual.backend.accesos.dominio.puerto;

import java.util.Optional;

/**
 * Datos de quien realiza la operacion actual. Fuera de una solicitud HTTP
 * (por ejemplo, tareas programadas) los valores pueden estar vacios.
 */
public interface ContextoSolicitud {

    Optional<UsuarioActual> usuarioActual();

    Optional<String> direccionIp();

    Optional<String> agenteUsuario();

    record UsuarioActual(Integer id, String nombreUsuario) {
    }
}
