package com.visual.backend.finanzas.infraestructura.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.finanzas.aplicacion.dto.ComandoProveedor;
import com.visual.backend.finanzas.aplicacion.dto.VistaProveedor;
import com.visual.backend.finanzas.aplicacion.servicio.ServicioProveedores;
import com.visual.backend.finanzas.dominio.modelo.Filtros;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/finanzas/proveedores")
public class ControladorProveedores {

    private final ServicioProveedores servicio;

    public ControladorProveedores(ServicioProveedores servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(ReglasAccesoFinanzas.VER_PROVEEDORES)
    public Pagina<VistaProveedor> listar(@RequestParam(required = false) String texto,
                                         @RequestParam(defaultValue = "0") int pagina,
                                         @RequestParam(defaultValue = "20") int tamano) {
        return servicio.listar(new Filtros.Proveedores(texto), new SolicitudPagina(pagina, tamano));
    }

    @GetMapping("/{id}")
    @PreAuthorize(ReglasAccesoFinanzas.VER_PROVEEDORES)
    public VistaProveedor obtener(@PathVariable Integer id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoFinanzas.GESTIONAR_PROVEEDORES)
    public VistaProveedor crear(@Valid @RequestBody ComandoProveedor comando) {
        return servicio.crear(comando);
    }

    @PutMapping("/{id}")
    @PreAuthorize(ReglasAccesoFinanzas.GESTIONAR_PROVEEDORES)
    public VistaProveedor actualizar(@PathVariable Integer id, @Valid @RequestBody ComandoProveedor comando) {
        return servicio.actualizar(id, comando);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ReglasAccesoFinanzas.GESTIONAR_PROVEEDORES)
    public void eliminar(@PathVariable Integer id) {
        servicio.eliminar(id);
    }
}
