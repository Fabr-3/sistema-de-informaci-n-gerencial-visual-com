package com.visual.backend.accesos.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.dto.ComandoActualizarUsuario;
import com.visual.backend.accesos.aplicacion.dto.ComandoCrearUsuario;
import com.visual.backend.accesos.aplicacion.dto.ComandoRestablecerContrasena;
import com.visual.backend.accesos.aplicacion.dto.VistaUsuario;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.modelo.DatosPersonales;
import com.visual.backend.accesos.dominio.modelo.FiltroUsuarios;
import com.visual.backend.accesos.dominio.modelo.PoliticaContrasena;
import com.visual.backend.accesos.dominio.modelo.Rol;
import com.visual.backend.accesos.dominio.modelo.Usuario;
import com.visual.backend.accesos.dominio.puerto.CodificadorContrasenas;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.accesos.dominio.puerto.RepositorioRoles;
import com.visual.backend.accesos.dominio.puerto.RepositorioTokensRefresco;
import com.visual.backend.accesos.dominio.puerto.RepositorioUsuarios;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.DatoDuplicadoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/**
 * Gestion de cuentas de usuario por el administrador. Toda modificacion se
 * audita y las que reducen el acceso cierran las sesiones abiertas.
 */
@Service
public class ServicioUsuarios {

    public static final String ENTIDAD = "usuarios";

    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioRoles repositorioRoles;
    private final RepositorioTokensRefresco repositorioTokens;
    private final CodificadorContrasenas codificador;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public ServicioUsuarios(RepositorioUsuarios repositorioUsuarios, RepositorioRoles repositorioRoles,
                            RepositorioTokensRefresco repositorioTokens, CodificadorContrasenas codificador,
                            ServicioAuditoria auditoria, ContextoSolicitud contexto, Clock reloj) {
        this.repositorioUsuarios = repositorioUsuarios;
        this.repositorioRoles = repositorioRoles;
        this.repositorioTokens = repositorioTokens;
        this.codificador = codificador;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Pagina<VistaUsuario> listar(FiltroUsuarios filtro, SolicitudPagina solicitud) {
        LocalDateTime ahora = ahora();
        return repositorioUsuarios.buscar(filtro, solicitud).mapear(usuario -> VistaUsuario.de(usuario, ahora));
    }

    @Transactional(readOnly = true)
    public VistaUsuario obtener(Integer id) {
        return VistaUsuario.de(buscarUsuario(id), ahora());
    }

    @Transactional
    public VistaUsuario crear(ComandoCrearUsuario comando) {
        LocalDateTime ahora = ahora();
        String nombreUsuario = Usuario.normalizarNombreUsuario(comando.nombreUsuario());
        DatosPersonales datos = new DatosPersonales(comando.nombres(), comando.apellidoPaterno(),
                comando.apellidoMaterno(), comando.email());

        if (repositorioUsuarios.existeNombreUsuario(nombreUsuario)) {
            throw new DatoDuplicadoExcepcion("El nombre de usuario " + nombreUsuario + " ya está registrado");
        }
        verificarEmailDisponible(datos.email(), null);
        PoliticaContrasena.validar(comando.contrasenaTemporal());

        Usuario usuario = Usuario.nuevo(nombreUsuario, datos, codificador.codificar(comando.contrasenaTemporal()),
                buscarRol(comando.idRol()), comando.fechaExpiracion(), idActor(), ahora);
        VistaUsuario creado = VistaUsuario.de(repositorioUsuarios.guardar(usuario), ahora);
        auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD, creado.id(), null,
                creado.comoMapaAuditoria(), null);
        return creado;
    }

    @Transactional
    public VistaUsuario actualizar(Integer id, ComandoActualizarUsuario comando) {
        LocalDateTime ahora = ahora();
        Usuario usuario = buscarUsuario(id);
        VistaUsuario anterior = VistaUsuario.de(usuario, ahora);
        DatosPersonales datos = new DatosPersonales(comando.nombres(), comando.apellidoPaterno(),
                comando.apellidoMaterno(), comando.email());
        Rol nuevoRol = buscarRol(comando.idRol());
        boolean cambiaRol = !nuevoRol.getId().equals(usuario.getRol().getId());

        verificarEmailDisponible(datos.email(), id);
        if (cambiaRol) {
            verificarNoEsActor(id, "No puede cambiar su propio rol");
            if (!nuevoRol.esAdministrador()) {
                verificarNoEsUltimoAdministrador(usuario, ahora);
            }
        }

        usuario.actualizar(datos, nuevoRol, comando.fechaExpiracion(), idActor(), ahora);
        VistaUsuario actualizado = VistaUsuario.de(repositorioUsuarios.guardar(usuario), ahora);
        if (cambiaRol) {
            // Los permisos viajan en las sesiones abiertas: se obliga a renovarlas
            repositorioTokens.revocarTodosDelUsuario(id, ahora);
        }
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id, anterior.comoMapaAuditoria(),
                actualizado.comoMapaAuditoria(), null);
        return actualizado;
    }

    @Transactional
    public VistaUsuario cambiarEstado(Integer id, boolean activo) {
        LocalDateTime ahora = ahora();
        Usuario usuario = buscarUsuario(id);
        VistaUsuario anterior = VistaUsuario.de(usuario, ahora);

        if (!activo) {
            verificarNoEsActor(id, "No puede desactivar su propia cuenta");
            verificarNoEsUltimoAdministrador(usuario, ahora);
        }

        usuario.cambiarEstado(activo, idActor(), ahora);
        VistaUsuario actualizado = VistaUsuario.de(repositorioUsuarios.guardar(usuario), ahora);
        if (!activo) {
            repositorioTokens.revocarTodosDelUsuario(id, ahora);
        }
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id, anterior.comoMapaAuditoria(),
                actualizado.comoMapaAuditoria(), activo ? "Activación de cuenta" : "Desactivación de cuenta");
        return actualizado;
    }

    /** Asigna una contrasena temporal y desbloquea la cuenta. No se audita el valor. */
    @Transactional
    public void restablecerContrasena(Integer id, ComandoRestablecerContrasena comando) {
        LocalDateTime ahora = ahora();
        Usuario usuario = buscarUsuario(id);
        PoliticaContrasena.validar(comando.contrasenaTemporal());

        usuario.restablecerContrasena(codificador.codificar(comando.contrasenaTemporal()), idActor(), ahora);
        repositorioUsuarios.guardar(usuario);
        repositorioTokens.revocarTodosDelUsuario(id, ahora);
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id, null, null,
                "Restablecimiento de contraseña");
    }

    @Transactional
    public void eliminar(Integer id) {
        LocalDateTime ahora = ahora();
        Usuario usuario = buscarUsuario(id);
        VistaUsuario anterior = VistaUsuario.de(usuario, ahora);

        verificarNoEsActor(id, "No puede eliminar su propia cuenta");
        verificarNoEsUltimoAdministrador(usuario, ahora);

        usuario.eliminar(idActor(), ahora);
        repositorioUsuarios.guardar(usuario);
        repositorioTokens.revocarTodosDelUsuario(id, ahora);
        auditoria.registrarOperacion(AccionAuditoria.ELIMINAR, ENTIDAD, id, anterior.comoMapaAuditoria(),
                null, null);
    }

    private Usuario buscarUsuario(Integer id) {
        return repositorioUsuarios.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el usuario", id));
    }

    private Rol buscarRol(Integer idRol) {
        return repositorioRoles.buscarPorId(idRol)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el rol", idRol));
    }

    private void verificarEmailDisponible(String email, Integer idExcluido) {
        if (repositorioUsuarios.existeEmail(email, idExcluido)) {
            throw new DatoDuplicadoExcepcion("El email " + email + " ya está registrado");
        }
    }

    /** Evita que el administrador se quite el acceso a si mismo. */
    private void verificarNoEsActor(Integer idUsuario, String mensaje) {
        if (Objects.equals(idUsuario, idActor())) {
            throw new ReglaNegocioExcepcion(mensaje);
        }
    }

    /** El sistema siempre debe conservar al menos un administrador habilitado. */
    private void verificarNoEsUltimoAdministrador(Usuario usuario, LocalDateTime ahora) {
        if (usuario.esAdministradorHabilitado(ahora)
                && repositorioUsuarios.contarOtrosAdministradoresHabilitados(usuario.getId(), ahora) == 0) {
            throw new ReglaNegocioExcepcion("Debe existir al menos un administrador habilitado");
        }
    }

    private Integer idActor() {
        return contexto.idUsuarioActual();
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }
}
