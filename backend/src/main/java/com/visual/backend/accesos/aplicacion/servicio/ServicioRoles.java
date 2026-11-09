package com.visual.backend.accesos.aplicacion.servicio;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.dto.VistaRol;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.modelo.Permiso;
import com.visual.backend.accesos.dominio.modelo.Rol;
import com.visual.backend.accesos.dominio.puerto.RepositorioPermisos;
import com.visual.backend.accesos.dominio.puerto.RepositorioRoles;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/**
 * Consulta de roles y permisos, y asignacion de permisos a roles. Los roles
 * y permisos son fijos; solo cambia la relacion entre ellos.
 */
@Service
public class ServicioRoles {

    public static final String ENTIDAD = "roles";

    private final RepositorioRoles repositorioRoles;
    private final RepositorioPermisos repositorioPermisos;
    private final ServicioAuditoria auditoria;

    public ServicioRoles(RepositorioRoles repositorioRoles, RepositorioPermisos repositorioPermisos,
                         ServicioAuditoria auditoria) {
        this.repositorioRoles = repositorioRoles;
        this.repositorioPermisos = repositorioPermisos;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<VistaRol> listarRoles() {
        return repositorioRoles.listar().stream().map(VistaRol::de).toList();
    }

    @Transactional(readOnly = true)
    public List<Permiso> listarPermisos() {
        return repositorioPermisos.listar();
    }

    /**
     * Reemplaza los permisos del rol. Los usuarios con sesion abierta reciben
     * los nuevos permisos al renovar su token de acceso.
     */
    @Transactional
    public VistaRol actualizarPermisos(Integer idRol, Set<Integer> idsPermisos) {
        Rol rol = repositorioRoles.buscarPorId(idRol)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el rol", idRol));
        List<Permiso> permisos = repositorioPermisos.buscarPorIds(idsPermisos);
        if (permisos.size() != idsPermisos.size()) {
            throw new ReglaNegocioExcepcion("Uno o más permisos no existen");
        }

        VistaRol anterior = VistaRol.de(rol);
        rol.reemplazarPermisos(new HashSet<>(permisos));
        VistaRol actualizado = VistaRol.de(repositorioRoles.guardarPermisos(rol));
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, idRol, anterior.comoMapaAuditoria(),
                actualizado.comoMapaAuditoria(), "Actualización de permisos");
        return actualizado;
    }
}
