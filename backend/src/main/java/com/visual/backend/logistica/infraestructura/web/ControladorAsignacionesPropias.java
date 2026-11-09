package com.visual.backend.logistica.infraestructura.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.logistica.aplicacion.dto.VistaMisAsignaciones;
import com.visual.backend.logistica.aplicacion.servicio.ServicioAsignacionesPropias;

/** Asignaciones del usuario autenticado (tecnicos y sub-contratados). */
@RestController
@RequestMapping("/api/logistica/mis-asignaciones")
public class ControladorAsignacionesPropias {

    private final ServicioAsignacionesPropias servicio;

    public ControladorAsignacionesPropias(ServicioAsignacionesPropias servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(ReglasAccesoLogistica.PROPIAS)
    public VistaMisAsignaciones mias() {
        return servicio.mias();
    }
}
