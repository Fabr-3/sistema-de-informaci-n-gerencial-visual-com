package com.visual.backend.gerencial.infraestructura.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.gerencial.aplicacion.servicio.ServicioKpis;
import com.visual.backend.gerencial.dominio.modelo.KpisGerenciales;

/** KPIs de informacion gerencial. */
@RestController
@RequestMapping("/api/gerencial/kpis")
public class ControladorKpis {

    private final ServicioKpis servicio;

    public ControladorKpis(ServicioKpis servicio) {
        this.servicio = servicio;
    }

    /** Sin fechas: el mes en curso hasta hoy. */
    @GetMapping
    @PreAuthorize(ReglasAccesoGerencial.VER)
    public KpisGerenciales kpis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return servicio.calcular(desde, hasta);
    }
}
