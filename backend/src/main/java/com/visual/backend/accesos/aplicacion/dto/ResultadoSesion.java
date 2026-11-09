package com.visual.backend.accesos.aplicacion.dto;

import java.time.Duration;

import com.visual.backend.accesos.dominio.puerto.EmisorTokensAcceso.TokenAcceso;

/**
 * Sesion abierta. El token de refresco viaja solo en una cookie HttpOnly; la
 * capa web nunca debe incluirlo en el cuerpo de la respuesta.
 */
public record ResultadoSesion(TokenAcceso tokenAcceso, String tokenRefresco, Duration duracionRefresco,
                              UsuarioSesion usuario) {

    @Override
    public String toString() {
        return "ResultadoSesion[usuario=" + usuario.nombreUsuario() + "]";
    }
}
