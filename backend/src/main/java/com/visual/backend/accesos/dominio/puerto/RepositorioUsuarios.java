package com.visual.backend.accesos.dominio.puerto;

import java.time.LocalDateTime;
import java.util.Optional;

import com.visual.backend.accesos.dominio.modelo.FiltroUsuarios;
import com.visual.backend.accesos.dominio.modelo.Usuario;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;

/** Persistencia de usuarios. Las consultas excluyen los usuarios eliminados. */
public interface RepositorioUsuarios {

    Optional<Usuario> buscarPorId(Integer id);

    Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario);

    boolean existeNombreUsuario(String nombreUsuario);

    /** Verifica si el email esta en uso por otro usuario distinto de idExcluido (puede ser nulo). */
    boolean existeEmail(String email, Integer idExcluido);

    boolean existeAdministrador();

    /** Cuenta administradores activos, no eliminados y vigentes en "ahora", distintos de idExcluido. */
    long contarOtrosAdministradoresHabilitados(Integer idExcluido, LocalDateTime ahora);

    Pagina<Usuario> buscar(FiltroUsuarios filtro, SolicitudPagina solicitud);

    Usuario guardar(Usuario usuario);
}
