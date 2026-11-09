package com.visual.backend.accesos.infraestructura.persistencia.adaptador;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import com.visual.backend.accesos.dominio.modelo.CodigoRol;
import com.visual.backend.accesos.dominio.modelo.FiltroUsuarios;
import com.visual.backend.accesos.dominio.modelo.Usuario;
import com.visual.backend.accesos.dominio.puerto.RepositorioUsuarios;
import com.visual.backend.accesos.infraestructura.persistencia.entidad.UsuarioEntidad;
import com.visual.backend.accesos.infraestructura.persistencia.jpa.RolJpaRepositorio;
import com.visual.backend.accesos.infraestructura.persistencia.jpa.UsuarioJpaRepositorio;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.infraestructura.persistencia.ConsultasJpa;

import jakarta.persistence.criteria.Predicate;

@Repository
public class RepositorioUsuariosJpa implements RepositorioUsuarios {

    private static final Sort ORDEN_POR_DEFECTO = Sort.by("apellidoPaterno", "nombres", "id");
    private static final List<String> CAMPOS_BUSQUEDA =
            List.of("nombreUsuario", "nombres", "apellidoPaterno", "apellidoMaterno", "email");

    private final UsuarioJpaRepositorio usuarios;
    private final RolJpaRepositorio roles;

    public RepositorioUsuariosJpa(UsuarioJpaRepositorio usuarios, RolJpaRepositorio roles) {
        this.usuarios = usuarios;
        this.roles = roles;
    }

    @Override
    public Optional<Usuario> buscarPorId(Integer id) {
        return usuarios.findByIdAndFechaEliminacionIsNull(id).map(MapeadorAccesos::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) {
        return usuarios.findByNombreUsuarioAndFechaEliminacionIsNull(nombreUsuario).map(MapeadorAccesos::aDominio);
    }

    @Override
    public boolean existeNombreUsuario(String nombreUsuario) {
        return usuarios.existsByNombreUsuario(nombreUsuario);
    }

    @Override
    public boolean existeEmail(String email, Integer idExcluido) {
        return idExcluido == null
                ? usuarios.existsByEmailIgnoreCase(email)
                : usuarios.existsByEmailIgnoreCaseAndIdNot(email, idExcluido);
    }

    @Override
    public boolean existeAdministrador() {
        return usuarios.existsByRolCodigoAndFechaEliminacionIsNull(CodigoRol.ADMINISTRADOR);
    }

    @Override
    public long contarOtrosAdministradoresHabilitados(Integer idExcluido, LocalDateTime ahora) {
        return usuarios.contarHabilitadosPorRolExcluyendo(CodigoRol.ADMINISTRADOR, idExcluido, ahora);
    }

    @Override
    public Pagina<Usuario> buscar(FiltroUsuarios filtro, SolicitudPagina solicitud) {
        return ConsultasJpa.pagina(usuarios.findAll(especificacion(filtro),
                ConsultasJpa.paginacion(solicitud, ORDEN_POR_DEFECTO)).map(MapeadorAccesos::aDominio));
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        UsuarioEntidad entidad = usuario.getId() == null
                ? new UsuarioEntidad()
                : usuarios.getReferenceById(usuario.getId());
        MapeadorAccesos.copiar(usuario, entidad, roles.getReferenceById(usuario.getRol().getId()));
        return MapeadorAccesos.aDominio(usuarios.save(entidad));
    }

    /** Excluye eliminados y aplica los filtros opcionales. */
    private static Specification<UsuarioEntidad> especificacion(FiltroUsuarios filtro) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.isNull(raiz.get("fechaEliminacion")));
            if (filtro.texto() != null && !filtro.texto().isBlank()) {
                String patron = ConsultasJpa.patronContiene(filtro.texto());
                condiciones.add(cb.or(CAMPOS_BUSQUEDA.stream()
                        .map(campo -> cb.like(cb.lower(raiz.get(campo)), patron, ConsultasJpa.ESCAPE_LIKE))
                        .toArray(Predicate[]::new)));
            }
            if (filtro.idRol() != null) {
                condiciones.add(cb.equal(raiz.get("rol").get("id"), filtro.idRol()));
            }
            if (filtro.activo() != null) {
                condiciones.add(cb.equal(raiz.get("activo"), filtro.activo()));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
