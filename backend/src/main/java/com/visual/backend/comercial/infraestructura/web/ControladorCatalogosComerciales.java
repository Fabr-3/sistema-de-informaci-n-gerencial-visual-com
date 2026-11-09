package com.visual.backend.comercial.infraestructura.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.comercial.aplicacion.dto.ComandoLugar;
import com.visual.backend.comercial.aplicacion.dto.ComandoServicio;
import com.visual.backend.comercial.aplicacion.servicio.ServicioCatalogosComerciales;
import com.visual.backend.comercial.dominio.modelo.ElementoCatalogo;
import com.visual.backend.comercial.dominio.modelo.Lugar;
import com.visual.backend.comercial.dominio.modelo.Servicio;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/comercial/catalogos")
public class ControladorCatalogosComerciales {

    private final ServicioCatalogosComerciales servicio;

    public ControladorCatalogosComerciales(ServicioCatalogosComerciales servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/tipos-cliente")
    @PreAuthorize(ReglasAccesoComercial.VER)
    public List<ElementoCatalogo> listarTiposCliente() {
        return servicio.listarTiposCliente();
    }

    @GetMapping("/tipos-documento")
    @PreAuthorize(ReglasAccesoComercial.VER)
    public List<ElementoCatalogo> listarTiposDocumento() {
        return servicio.listarTiposDocumento();
    }

    @GetMapping("/tipos-contacto")
    @PreAuthorize(ReglasAccesoComercial.VER)
    public List<ElementoCatalogo> listarTiposContacto() {
        return servicio.listarTiposContacto();
    }

    @GetMapping("/servicios")
    @PreAuthorize(ReglasAccesoComercial.VER)
    public List<Servicio> listarServicios(@RequestParam(defaultValue = "false") boolean soloActivos) {
        return servicio.listarServicios(soloActivos);
    }

    @PostMapping("/servicios")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoComercial.GESTIONAR)
    public Servicio crearServicio(@Valid @RequestBody ComandoServicio comando) {
        return servicio.guardarServicio(null, comando);
    }

    @PutMapping("/servicios/{id}")
    @PreAuthorize(ReglasAccesoComercial.GESTIONAR)
    public Servicio actualizarServicio(@PathVariable Integer id, @Valid @RequestBody ComandoServicio comando) {
        return servicio.guardarServicio(id, comando);
    }

    @GetMapping("/lugares")
    @PreAuthorize(ReglasAccesoComercial.VER)
    public List<Lugar> listarLugares() {
        return servicio.listarLugares();
    }

    @PostMapping("/lugares")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoComercial.GESTIONAR)
    public Lugar crearLugar(@Valid @RequestBody ComandoLugar comando) {
        return servicio.guardarLugar(null, comando);
    }

    @PutMapping("/lugares/{id}")
    @PreAuthorize(ReglasAccesoComercial.GESTIONAR)
    public Lugar actualizarLugar(@PathVariable Integer id, @Valid @RequestBody ComandoLugar comando) {
        return servicio.guardarLugar(id, comando);
    }
}
