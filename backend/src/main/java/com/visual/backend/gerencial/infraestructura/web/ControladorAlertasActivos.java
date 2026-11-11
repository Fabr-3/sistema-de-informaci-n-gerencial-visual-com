package com.visual.backend.gerencial.infraestructura.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.gerencial.aplicacion.servicio.ServicioAlertasActivos;
import com.visual.backend.gerencial.dominio.modelo.AlertaActivo;

/** Alertas vigentes del estado de los activos. */
@RestController
@RequestMapping("/api/gerencial/alertas-activos")
public class ControladorAlertasActivos {

    private final ServicioAlertasActivos servicio;

    public ControladorAlertasActivos(ServicioAlertasActivos servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(ReglasAccesoGerencial.VER)
    public List<AlertaActivo> listar() {
        return servicio.listar();
    }
}
