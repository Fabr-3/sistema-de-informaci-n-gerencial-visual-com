package com.visual.backend.accesos.infraestructura.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.accesos.aplicacion.dto.ComandoPermisosRol;
import com.visual.backend.accesos.aplicacion.dto.VistaRol;
import com.visual.backend.accesos.aplicacion.servicio.ServicioRoles;
import com.visual.backend.accesos.dominio.modelo.Permiso;
import com.visual.backend.accesos.dominio.modelo.Permisos;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ControladorRoles {

    private static final String PUEDE_VER = "hasAuthority('" + Permisos.ROLES_VER + "')";
    private static final String PUEDE_GESTIONAR = "hasAuthority('" + Permisos.ROLES_GESTIONAR + "')";
    /** La gestion de usuarios necesita la lista de roles (filtro y formulario), sin editar permisos. */
    private static final String PUEDE_VER_ROLES = "hasAnyAuthority('" + Permisos.ROLES_VER + "', '"
            + Permisos.USUARIOS_VER + "', '" + Permisos.USUARIOS_GESTIONAR + "')";

    private final ServicioRoles servicio;

    public ControladorRoles(ServicioRoles servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/roles")
    @PreAuthorize(PUEDE_VER_ROLES)
    public List<VistaRol> listarRoles() {
        return servicio.listarRoles();
    }

    @GetMapping("/permisos")
    @PreAuthorize(PUEDE_VER)
    public List<Permiso> listarPermisos() {
        return servicio.listarPermisos();
    }

    @PutMapping("/roles/{id}/permisos")
    @PreAuthorize(PUEDE_GESTIONAR)
    public VistaRol actualizarPermisos(@PathVariable Integer id, @Valid @RequestBody ComandoPermisosRol comando) {
        return servicio.actualizarPermisos(id, comando.idsPermisos());
    }
}
