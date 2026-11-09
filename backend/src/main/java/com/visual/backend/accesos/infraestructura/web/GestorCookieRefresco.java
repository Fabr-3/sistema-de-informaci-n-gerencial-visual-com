package com.visual.backend.accesos.infraestructura.web;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.visual.backend.accesos.infraestructura.seguridad.PropiedadesSeguridad;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Cookie del token de refresco: HttpOnly (inaccesible desde JavaScript, lo
 * que mitiga XSS), SameSite=Strict (mitiga CSRF) y limitada a /api/auth.
 */
@Component
public class GestorCookieRefresco {

    private static final String SAME_SITE = "Strict";

    private final PropiedadesSeguridad.Refresco configuracion;

    public GestorCookieRefresco(PropiedadesSeguridad propiedades) {
        this.configuracion = propiedades.refresco();
    }

    public ResponseCookie crear(String valor, Duration duracion) {
        return construir(valor, duracion);
    }

    public ResponseCookie eliminar() {
        return construir("", Duration.ZERO);
    }

    public Optional<String> leer(HttpServletRequest solicitud) {
        Cookie[] cookies = solicitud.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> configuracion.nombreCookie().equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private ResponseCookie construir(String valor, Duration duracion) {
        return ResponseCookie.from(configuracion.nombreCookie(), valor)
                .httpOnly(true)
                .secure(configuracion.cookieSegura())
                .sameSite(SAME_SITE)
                .path(ControladorAutenticacion.RUTA_BASE)
                .maxAge(duracion)
                .build();
    }
}
