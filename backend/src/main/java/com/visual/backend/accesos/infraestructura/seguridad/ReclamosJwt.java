package com.visual.backend.accesos.infraestructura.seguridad;

/** Nombres de los reclamos propios incluidos en el token de acceso. */
final class ReclamosJwt {

    static final String NOMBRE_USUARIO = "nombre_usuario";
    static final String ROL = "rol";
    static final String PERMISOS = "permisos";

    private ReclamosJwt() {
    }
}
