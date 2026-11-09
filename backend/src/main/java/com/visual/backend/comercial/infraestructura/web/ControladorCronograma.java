package com.visual.backend.comercial.infraestructura.web;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.comercial.aplicacion.servicio.ServicioEventos;
import com.visual.backend.comercial.dominio.modelo.EventoCalendario;
import com.visual.backend.comercial.dominio.modelo.Filtros;

/** Eventos de todos los proyectos para el calendario (cronograma general). */
@RestController
@RequestMapping("/api/comercial/cronograma")
public class ControladorCronograma {

    private final ServicioEventos servicio;

    public ControladorCronograma(ServicioEventos servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(ReglasAccesoComercial.VER)
    public List<EventoCalendario> listar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) Integer idProyecto) {
        return servicio.calendario(new Filtros.Eventos(desde, hasta, idProyecto));
    }
}
