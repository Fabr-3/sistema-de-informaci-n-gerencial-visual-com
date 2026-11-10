package com.visual.backend.finanzas.infraestructura.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.finanzas.aplicacion.servicio.ServicioIndicadores;
import com.visual.backend.finanzas.dominio.modelo.IndicadoresFinancieros;

/** Tablero financiero del gerente. */
@RestController
@RequestMapping("/api/finanzas/indicadores")
public class ControladorIndicadores {

    private final ServicioIndicadores servicio;

    public ControladorIndicadores(ServicioIndicadores servicio) {
        this.servicio = servicio;
    }

    /** Sin fechas: el mes en curso hasta hoy. */
    @GetMapping
    @PreAuthorize(ReglasAccesoFinanzas.VER)
    public IndicadoresFinancieros indicadores(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return servicio.calcular(desde, hasta);
    }
}
