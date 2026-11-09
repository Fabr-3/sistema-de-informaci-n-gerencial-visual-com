package com.visual.backend.logistica.infraestructura.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.logistica.aplicacion.dto.CatalogosLogistica;
import com.visual.backend.logistica.aplicacion.servicio.ServicioCatalogosLogistica;

@RestController
@RequestMapping("/api/logistica/catalogos")
public class ControladorCatalogosLogistica {

    private final ServicioCatalogosLogistica servicio;

    public ControladorCatalogosLogistica(ServicioCatalogosLogistica servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(ReglasAccesoLogistica.VER)
    public CatalogosLogistica catalogos() {
        return servicio.catalogos();
    }
}
