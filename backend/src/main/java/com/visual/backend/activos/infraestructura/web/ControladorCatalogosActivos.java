package com.visual.backend.activos.infraestructura.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.activos.aplicacion.dto.ComandoBodega;
import com.visual.backend.activos.aplicacion.dto.ComandoCatalogo;
import com.visual.backend.activos.aplicacion.servicio.ServicioCatalogosActivos;
import com.visual.backend.activos.dominio.modelo.Bodega;
import com.visual.backend.activos.dominio.modelo.Categoria;
import com.visual.backend.activos.dominio.modelo.Fabricante;
import com.visual.backend.activos.dominio.modelo.TipoMantenimiento;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/activos/catalogos")
public class ControladorCatalogosActivos {

    private final ServicioCatalogosActivos servicio;

    public ControladorCatalogosActivos(ServicioCatalogosActivos servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/categorias")
    @PreAuthorize(ReglasAccesoActivos.VER)
    public List<Categoria> listarCategorias() {
        return servicio.listarCategorias();
    }

    @PostMapping("/categorias")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoActivos.GESTIONAR)
    public Categoria crearCategoria(@Valid @RequestBody ComandoCatalogo comando) {
        return servicio.guardarCategoria(null, comando);
    }

    @PutMapping("/categorias/{id}")
    @PreAuthorize(ReglasAccesoActivos.GESTIONAR)
    public Categoria actualizarCategoria(@PathVariable Integer id, @Valid @RequestBody ComandoCatalogo comando) {
        return servicio.guardarCategoria(id, comando);
    }

    @GetMapping("/fabricantes")
    @PreAuthorize(ReglasAccesoActivos.VER)
    public List<Fabricante> listarFabricantes() {
        return servicio.listarFabricantes();
    }

    @PostMapping("/fabricantes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoActivos.GESTIONAR)
    public Fabricante crearFabricante(@Valid @RequestBody ComandoCatalogo comando) {
        return servicio.guardarFabricante(null, comando);
    }

    @PutMapping("/fabricantes/{id}")
    @PreAuthorize(ReglasAccesoActivos.GESTIONAR)
    public Fabricante actualizarFabricante(@PathVariable Integer id, @Valid @RequestBody ComandoCatalogo comando) {
        return servicio.guardarFabricante(id, comando);
    }

    @GetMapping("/bodegas")
    @PreAuthorize(ReglasAccesoActivos.VER)
    public List<Bodega> listarBodegas() {
        return servicio.listarBodegas();
    }

    @PostMapping("/bodegas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoActivos.GESTIONAR)
    public Bodega crearBodega(@Valid @RequestBody ComandoBodega comando) {
        return servicio.guardarBodega(null, comando);
    }

    @PutMapping("/bodegas/{id}")
    @PreAuthorize(ReglasAccesoActivos.GESTIONAR)
    public Bodega actualizarBodega(@PathVariable Integer id, @Valid @RequestBody ComandoBodega comando) {
        return servicio.guardarBodega(id, comando);
    }

    @GetMapping("/tipos-mantenimiento")
    @PreAuthorize(ReglasAccesoActivos.VER)
    public List<TipoMantenimiento> listarTiposMantenimiento() {
        return servicio.listarTiposMantenimiento();
    }
}
