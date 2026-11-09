package com.visual.backend.accesos.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.dto.ComandoCambioContrasena;
import com.visual.backend.accesos.aplicacion.dto.ComandoInicioSesion;
import com.visual.backend.accesos.aplicacion.dto.ResultadoSesion;
import com.visual.backend.accesos.aplicacion.dto.UsuarioSesion;
import com.visual.backend.accesos.dominio.excepcion.AutenticacionExcepcion;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.modelo.PoliticaBloqueo;
import com.visual.backend.accesos.dominio.modelo.PoliticaContrasena;
import com.visual.backend.accesos.dominio.modelo.TokenRefresco;
import com.visual.backend.accesos.dominio.modelo.Usuario;
import com.visual.backend.accesos.dominio.puerto.CodificadorContrasenas;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.accesos.dominio.puerto.EmisorTokensAcceso;
import com.visual.backend.accesos.dominio.puerto.GeneradorTokensRefresco;
import com.visual.backend.accesos.dominio.puerto.RepositorioTokensRefresco;
import com.visual.backend.accesos.dominio.puerto.RepositorioUsuarios;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/**
 * Casos de uso de sesion: inicio, refresco con rotacion de token, cierre y
 * cambio de la contrasena propia.
 *
 * Los fallos de autenticacion no deshacen la transaccion para que persistan
 * el contador de intentos, el bloqueo y el registro de auditoria.
 */
@Service
public class ServicioAutenticacion {

    private static final String CONTRASENA_SIMULADA = "contrasena-simulada-0";

    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioTokensRefresco repositorioTokens;
    private final CodificadorContrasenas codificador;
    private final EmisorTokensAcceso emisorTokens;
    private final GeneradorTokensRefresco generadorTokensRefresco;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final PoliticaBloqueo politicaBloqueo;
    private final ParametrosSesion parametrosSesion;
    private final Clock reloj;
    /**
     * Hash usado cuando el usuario no existe: se compara igual para que el
     * tiempo de respuesta no revele que usuarios estan registrados.
     */
    private final String hashSimulado;

    public ServicioAutenticacion(RepositorioUsuarios repositorioUsuarios, RepositorioTokensRefresco repositorioTokens,
                                 CodificadorContrasenas codificador, EmisorTokensAcceso emisorTokens,
                                 GeneradorTokensRefresco generadorTokensRefresco, ServicioAuditoria auditoria,
                                 ContextoSolicitud contexto, PoliticaBloqueo politicaBloqueo,
                                 ParametrosSesion parametrosSesion, Clock reloj) {
        this.repositorioUsuarios = repositorioUsuarios;
        this.repositorioTokens = repositorioTokens;
        this.codificador = codificador;
        this.emisorTokens = emisorTokens;
        this.generadorTokensRefresco = generadorTokensRefresco;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.politicaBloqueo = politicaBloqueo;
        this.parametrosSesion = parametrosSesion;
        this.reloj = reloj;
        this.hashSimulado = codificador.codificar(CONTRASENA_SIMULADA);
    }

    @Transactional(noRollbackFor = AutenticacionExcepcion.class)
    public ResultadoSesion iniciarSesion(ComandoInicioSesion comando) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        String nombreUsuario = comando.nombreUsuario().trim().toLowerCase(Locale.ROOT);
        Optional<Usuario> encontrado = repositorioUsuarios.buscarPorNombreUsuario(nombreUsuario);

        if (encontrado.isEmpty()) {
            codificador.coincide(comando.contrasena(), hashSimulado);
            throw rechazarInicio(null, nombreUsuario, "Usuario inexistente", AutenticacionExcepcion.CREDENCIALES_INVALIDAS);
        }
        Usuario usuario = encontrado.get();

        if (usuario.estaBloqueado(ahora)) {
            throw rechazarInicio(usuario, "Cuenta bloqueada", AutenticacionExcepcion.CUENTA_BLOQUEADA);
        }
        if (!codificador.coincide(comando.contrasena(), usuario.getHashContrasena())) {
            throw registrarContrasenaIncorrecta(usuario, ahora);
        }
        if (!usuario.estaHabilitado(ahora)) {
            throw rechazarInicio(usuario, "Cuenta inactiva o expirada", AutenticacionExcepcion.CUENTA_NO_HABILITADA);
        }

        usuario.registrarAccesoExitoso(ahora);
        repositorioUsuarios.guardar(usuario);
        auditoria.registrarAcceso(AccionAuditoria.INICIO_SESION, usuario.getId(), usuario.getNombreUsuario(), null);
        return abrirSesion(usuario, ahora);
    }

    /**
     * Rota el token de refresco: el usado se revoca y se emite uno nuevo. Si
     * llega un token ya revocado se asume robo y se cierran todas las
     * sesiones del usuario.
     */
    @Transactional(noRollbackFor = AutenticacionExcepcion.class)
    public ResultadoSesion refrescarSesion(String tokenRefresco) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        TokenRefresco token = buscarToken(tokenRefresco)
                .orElseThrow(() -> new AutenticacionExcepcion(AutenticacionExcepcion.SESION_INVALIDA));

        if (token.estaRevocado()) {
            repositorioTokens.revocarTodosDelUsuario(token.getIdUsuario(), ahora);
            throw new AutenticacionExcepcion(AutenticacionExcepcion.SESION_INVALIDA);
        }
        if (!token.estaVigente(ahora)) {
            throw new AutenticacionExcepcion(AutenticacionExcepcion.SESION_INVALIDA);
        }

        token.revocar(ahora);
        repositorioTokens.guardar(token);

        Usuario usuario = repositorioUsuarios.buscarPorId(token.getIdUsuario())
                .filter(encontrado -> encontrado.estaHabilitado(ahora) && !encontrado.estaBloqueado(ahora))
                .orElseThrow(() -> new AutenticacionExcepcion(AutenticacionExcepcion.SESION_INVALIDA));
        return abrirSesion(usuario, ahora);
    }

    /** Revoca el token de refresco recibido. Es idempotente: no falla si ya no es valido. */
    @Transactional
    public void cerrarSesion(String tokenRefresco) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        buscarToken(tokenRefresco)
                .filter(token -> !token.estaRevocado())
                .ifPresent(token -> {
                    token.revocar(ahora);
                    repositorioTokens.guardar(token);
                    repositorioUsuarios.buscarPorId(token.getIdUsuario()).ifPresent(usuario ->
                            auditoria.registrarAcceso(AccionAuditoria.CIERRE_SESION, usuario.getId(),
                                    usuario.getNombreUsuario(), null));
                });
    }

    /**
     * Cambio de contrasena del usuario autenticado. Cierra sus demas sesiones
     * y abre una nueva con los permisos completos.
     */
    @Transactional
    public ResultadoSesion cambiarContrasenaPropia(ComandoCambioContrasena comando) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Usuario usuario = contexto.usuarioActual()
                .flatMap(actual -> repositorioUsuarios.buscarPorId(actual.id()))
                .orElseThrow(() -> new AutenticacionExcepcion(AutenticacionExcepcion.SESION_INVALIDA));

        if (!codificador.coincide(comando.contrasenaActual(), usuario.getHashContrasena())) {
            throw new ReglaNegocioExcepcion("La contraseña actual es incorrecta");
        }
        if (comando.contrasenaActual().equals(comando.nuevaContrasena())) {
            throw new ReglaNegocioExcepcion("La nueva contraseña debe ser distinta de la actual");
        }
        PoliticaContrasena.validar(comando.nuevaContrasena());

        usuario.cambiarContrasena(codificador.codificar(comando.nuevaContrasena()), ahora);
        repositorioUsuarios.guardar(usuario);
        repositorioTokens.revocarTodosDelUsuario(usuario.getId(), ahora);
        auditoria.registrarAcceso(AccionAuditoria.CAMBIO_CONTRASENA, usuario.getId(), usuario.getNombreUsuario(), null);
        return abrirSesion(usuario, ahora);
    }

    private ResultadoSesion abrirSesion(Usuario usuario, LocalDateTime ahora) {
        String valorRefresco = generadorTokensRefresco.generar();
        repositorioTokens.guardar(TokenRefresco.nuevo(usuario.getId(),
                generadorTokensRefresco.calcularHash(valorRefresco), ahora,
                ahora.plus(parametrosSesion.duracionRefresco())));
        return new ResultadoSesion(emisorTokens.emitir(usuario), valorRefresco,
                parametrosSesion.duracionRefresco(), UsuarioSesion.de(usuario));
    }

    private Optional<TokenRefresco> buscarToken(String tokenRefresco) {
        if (tokenRefresco == null || tokenRefresco.isBlank()) {
            return Optional.empty();
        }
        return repositorioTokens.buscarPorHash(generadorTokensRefresco.calcularHash(tokenRefresco));
    }

    private AutenticacionExcepcion registrarContrasenaIncorrecta(Usuario usuario, LocalDateTime ahora) {
        boolean quedoBloqueado = usuario.registrarIntentoFallido(politicaBloqueo, ahora);
        repositorioUsuarios.guardar(usuario);
        if (quedoBloqueado) {
            auditoria.registrarAcceso(AccionAuditoria.BLOQUEO_CUENTA, usuario.getId(), usuario.getNombreUsuario(),
                    "Bloqueo por " + politicaBloqueo.intentosMaximos() + " intentos fallidos");
        }
        return rechazarInicio(usuario, "Contraseña incorrecta", AutenticacionExcepcion.CREDENCIALES_INVALIDAS);
    }

    private AutenticacionExcepcion rechazarInicio(Usuario usuario, String motivo, String mensaje) {
        return rechazarInicio(usuario.getId(), usuario.getNombreUsuario(), motivo, mensaje);
    }

    /** Audita el intento fallido y devuelve la excepcion que debe lanzar el llamador. */
    private AutenticacionExcepcion rechazarInicio(Integer idUsuario, String nombreUsuario, String motivo,
                                                  String mensaje) {
        auditoria.registrarAcceso(AccionAuditoria.INICIO_SESION_FALLIDO, idUsuario, nombreUsuario, motivo);
        return new AutenticacionExcepcion(mensaje);
    }
}
