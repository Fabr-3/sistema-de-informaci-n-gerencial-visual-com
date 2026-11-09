package com.visual.backend.accesos.infraestructura.seguridad;

import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Obtiene el usuario del token de acceso y los datos de red de la solicitud
 * HTTP en curso. Se usa la IP de la conexion (no X-Forwarded-For) porque la
 * aplicacion no esta detras de un proxy de confianza.
 */
@Component
public class ContextoSolicitudSpring implements ContextoSolicitud {

    @Override
    public Optional<UsuarioActual> usuarioActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken tokenJwt) {
            return Optional.of(new UsuarioActual(Integer.valueOf(tokenJwt.getToken().getSubject()),
                    tokenJwt.getToken().getClaimAsString(ReclamosJwt.NOMBRE_USUARIO)));
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> direccionIp() {
        return solicitudActual().map(HttpServletRequest::getRemoteAddr);
    }

    @Override
    public Optional<String> agenteUsuario() {
        return solicitudActual().map(solicitud -> solicitud.getHeader(HttpHeaders.USER_AGENT));
    }

    private static Optional<HttpServletRequest> solicitudActual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            return Optional.of(atributos.getRequest());
        }
        return Optional.empty();
    }
}
