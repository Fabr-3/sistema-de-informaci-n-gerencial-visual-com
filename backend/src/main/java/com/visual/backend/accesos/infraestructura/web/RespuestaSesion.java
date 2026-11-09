package com.visual.backend.accesos.infraestructura.web;

import java.time.Instant;

import com.visual.backend.accesos.aplicacion.dto.ResultadoSesion;
import com.visual.backend.accesos.aplicacion.dto.UsuarioSesion;

/** Cuerpo de respuesta de sesion. Excluye a proposito el token de refresco. */
public record RespuestaSesion(String tokenAcceso, Instant expiraEn, UsuarioSesion usuario) {

    static RespuestaSesion de(ResultadoSesion resultado) {
        return new RespuestaSesion(resultado.tokenAcceso().valor(), resultado.tokenAcceso().expiraEn(),
                resultado.usuario());
    }
}
