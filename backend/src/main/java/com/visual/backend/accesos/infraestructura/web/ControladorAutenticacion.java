package com.visual.backend.accesos.infraestructura.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.accesos.aplicacion.dto.ComandoCambioContrasena;
import com.visual.backend.accesos.aplicacion.dto.ComandoInicioSesion;
import com.visual.backend.accesos.aplicacion.dto.ResultadoSesion;
import com.visual.backend.accesos.aplicacion.servicio.ServicioAutenticacion;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping(ControladorAutenticacion.RUTA_BASE)
public class ControladorAutenticacion {

    public static final String RUTA_BASE = "/api/auth";
    public static final String RUTA_INICIO_SESION = RUTA_BASE + "/inicio-sesion";
    public static final String RUTA_REFRESCO = RUTA_BASE + "/refresco";
    public static final String RUTA_CIERRE_SESION = RUTA_BASE + "/cierre-sesion";

    private final ServicioAutenticacion servicio;
    private final GestorCookieRefresco cookieRefresco;

    public ControladorAutenticacion(ServicioAutenticacion servicio, GestorCookieRefresco cookieRefresco) {
        this.servicio = servicio;
        this.cookieRefresco = cookieRefresco;
    }

    @PostMapping("/inicio-sesion")
    public ResponseEntity<RespuestaSesion> iniciarSesion(@Valid @RequestBody ComandoInicioSesion comando) {
        return responderSesion(servicio.iniciarSesion(comando));
    }

    @PostMapping("/refresco")
    public ResponseEntity<RespuestaSesion> refrescar(HttpServletRequest solicitud) {
        return responderSesion(servicio.refrescarSesion(cookieRefresco.leer(solicitud).orElse(null)));
    }

    @PostMapping("/cierre-sesion")
    public ResponseEntity<Void> cerrarSesion(HttpServletRequest solicitud) {
        cookieRefresco.leer(solicitud).ifPresent(servicio::cerrarSesion);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieRefresco.eliminar().toString())
                .build();
    }

    /** Disponible para cualquier usuario autenticado, incluso con contrasena temporal. */
    @PutMapping("/contrasena")
    public ResponseEntity<RespuestaSesion> cambiarContrasena(@Valid @RequestBody ComandoCambioContrasena comando) {
        return responderSesion(servicio.cambiarContrasenaPropia(comando));
    }

    private ResponseEntity<RespuestaSesion> responderSesion(ResultadoSesion resultado) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        cookieRefresco.crear(resultado.tokenRefresco(), resultado.duracionRefresco()).toString())
                .body(RespuestaSesion.de(resultado));
    }
}
