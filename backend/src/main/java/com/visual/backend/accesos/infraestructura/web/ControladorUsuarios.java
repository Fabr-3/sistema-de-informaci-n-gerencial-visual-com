package com.visual.backend.accesos.infraestructura.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.accesos.aplicacion.dto.ComandoActualizarUsuario;
import com.visual.backend.accesos.aplicacion.dto.ComandoCambioEstado;
import com.visual.backend.accesos.aplicacion.dto.ComandoCrearUsuario;
import com.visual.backend.accesos.aplicacion.dto.ComandoRestablecerContrasena;
import com.visual.backend.accesos.aplicacion.dto.VistaUsuario;
import com.visual.backend.accesos.aplicacion.servicio.ServicioUsuarios;
import com.visual.backend.accesos.dominio.modelo.FiltroUsuarios;
import com.visual.backend.accesos.dominio.modelo.Permisos;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios")
public class ControladorUsuarios {

    private static final String PUEDE_VER = "hasAuthority('" + Permisos.USUARIOS_VER + "')";
    private static final String PUEDE_GESTIONAR = "hasAuthority('" + Permisos.USUARIOS_GESTIONAR + "')";

    private final ServicioUsuarios servicio;

    public ControladorUsuarios(ServicioUsuarios servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(PUEDE_VER)
    public Pagina<VistaUsuario> listar(@RequestParam(required = false) String texto,
                                       @RequestParam(required = false) Integer idRol,
                                       @RequestParam(required = false) Boolean activo,
                                       @RequestParam(defaultValue = "0") int pagina,
                                       @RequestParam(defaultValue = "20") int tamano) {
        return servicio.listar(new FiltroUsuarios(texto, idRol, activo), new SolicitudPagina(pagina, tamano));
    }

    @GetMapping("/{id}")
    @PreAuthorize(PUEDE_VER)
    public VistaUsuario obtener(@PathVariable Integer id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(PUEDE_GESTIONAR)
    public VistaUsuario crear(@Valid @RequestBody ComandoCrearUsuario comando) {
        return servicio.crear(comando);
    }

    @PutMapping("/{id}")
    @PreAuthorize(PUEDE_GESTIONAR)
    public VistaUsuario actualizar(@PathVariable Integer id, @Valid @RequestBody ComandoActualizarUsuario comando) {
        return servicio.actualizar(id, comando);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize(PUEDE_GESTIONAR)
    public VistaUsuario cambiarEstado(@PathVariable Integer id, @Valid @RequestBody ComandoCambioEstado comando) {
        return servicio.cambiarEstado(id, comando.activo());
    }

    @PutMapping("/{id}/contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(PUEDE_GESTIONAR)
    public void restablecerContrasena(@PathVariable Integer id,
                                      @Valid @RequestBody ComandoRestablecerContrasena comando) {
        servicio.restablecerContrasena(id, comando);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(PUEDE_GESTIONAR)
    public void eliminar(@PathVariable Integer id) {
        servicio.eliminar(id);
    }
}
