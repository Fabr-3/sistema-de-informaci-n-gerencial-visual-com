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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.activos.aplicacion.dto.ComandoModelo;
import com.visual.backend.activos.aplicacion.dto.VistaModelo;
import com.visual.backend.activos.aplicacion.servicio.ServicioModelos;
import com.visual.backend.activos.dominio.modelo.TipoControl;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/activos/modelos")
public class ControladorModelos {

    private final ServicioModelos servicio;

    public ControladorModelos(ServicioModelos servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(ReglasAccesoActivos.VER)
    public List<VistaModelo> listar(@RequestParam(required = false) TipoControl tipoControl) {
        return servicio.listar(tipoControl);
    }

    @GetMapping("/{id}")
    @PreAuthorize(ReglasAccesoActivos.VER)
    public VistaModelo obtener(@PathVariable Integer id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoActivos.GESTIONAR)
    public VistaModelo crear(@Valid @RequestBody ComandoModelo comando) {
        return servicio.crear(comando);
    }

    @PutMapping("/{id}")
    @PreAuthorize(ReglasAccesoActivos.GESTIONAR)
    public VistaModelo actualizar(@PathVariable Integer id, @Valid @RequestBody ComandoModelo comando) {
        return servicio.actualizar(id, comando);
    }
}
