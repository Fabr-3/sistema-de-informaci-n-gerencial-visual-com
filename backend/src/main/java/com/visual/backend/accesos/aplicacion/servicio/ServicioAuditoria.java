package com.visual.backend.accesos.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.modelo.FiltroAuditoria;
import com.visual.backend.accesos.dominio.modelo.RegistroAuditoria;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud.UsuarioActual;
import com.visual.backend.accesos.dominio.puerto.RepositorioAuditoria;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;

/**
 * Registro y consulta de auditoria. Los registros se escriben dentro de la
 * transaccion de la operacion auditada para que ambos se confirmen juntos.
 * Es reutilizable por los demas modulos del sistema.
 */
@Service
public class ServicioAuditoria {

    /** Nombre usado cuando la operacion la ejecuta el propio sistema (tareas programadas). */
    public static final String ACTOR_SISTEMA = "sistema";
    private static final int LONGITUD_MAXIMA_NOMBRE = 50;
    private static final int LONGITUD_MAXIMA_AGENTE = 255;

    private final RepositorioAuditoria repositorio;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public ServicioAuditoria(RepositorioAuditoria repositorio, ContextoSolicitud contexto, Clock reloj) {
        this.repositorio = repositorio;
        this.contexto = contexto;
        this.reloj = reloj;
    }

    /** Audita una operacion CRUD realizada por el usuario autenticado. */
    public void registrarOperacion(AccionAuditoria accion, String entidad, Object idEntidad,
                                   Map<String, Object> valoresAnteriores, Map<String, Object> valoresNuevos,
                                   String detalle) {
        UsuarioActual actor = contexto.usuarioActual().orElse(null);
        registrar(accion, actor == null ? null : actor.id(),
                actor == null ? ACTOR_SISTEMA : actor.nombreUsuario(),
                entidad, idEntidad, valoresAnteriores, valoresNuevos, detalle);
    }

    /**
     * Audita eventos de acceso. Se indica el usuario explicitamente porque
     * durante el inicio de sesion aun no hay usuario autenticado.
     */
    public void registrarAcceso(AccionAuditoria accion, Integer idUsuario, String nombreUsuario, String detalle) {
        registrar(accion, idUsuario, nombreUsuario, null, null, null, null, detalle);
    }

    @Transactional(readOnly = true)
    public Pagina<RegistroAuditoria> consultar(FiltroAuditoria filtro, SolicitudPagina solicitud) {
        return repositorio.buscar(filtro, solicitud);
    }

    private void registrar(AccionAuditoria accion, Integer idUsuario, String nombreUsuario, String entidad,
                           Object idEntidad, Map<String, Object> anteriores, Map<String, Object> nuevos,
                           String detalle) {
        repositorio.registrar(new RegistroAuditoria(
                null,
                LocalDateTime.now(reloj),
                idUsuario,
                truncar(nombreUsuario, LONGITUD_MAXIMA_NOMBRE),
                accion,
                entidad,
                idEntidad == null ? null : idEntidad.toString(),
                anteriores,
                nuevos,
                detalle,
                contexto.direccionIp().orElse(null),
                truncar(contexto.agenteUsuario().orElse(null), LONGITUD_MAXIMA_AGENTE)));
    }

    private static String truncar(String valor, int longitudMaxima) {
        return valor == null || valor.length() <= longitudMaxima ? valor : valor.substring(0, longitudMaxima);
    }
}
