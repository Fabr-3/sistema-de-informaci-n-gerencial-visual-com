package com.visual.backend.activos.infraestructura.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.activos.aplicacion.dto.ComandoUsoGrupal;
import com.visual.backend.activos.aplicacion.servicio.ServicioUsoActivos;
import com.visual.backend.activos.dominio.modelo.RegistroUso;

import jakarta.validation.Valid;

/** Registro de uso de varios equipos a la vez (uso grupal). */
@RestController
@RequestMapping("/api/activos/usos-grupales")
public class ControladorUsosGrupales {

    private final ServicioUsoActivos usos;

    public ControladorUsosGrupales(ServicioUsoActivos usos) {
        this.usos = usos;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoActivos.REGISTRAR_MANTENIMIENTO)
    public List<RegistroUso> registrar(@Valid @RequestBody ComandoUsoGrupal comando) {
        return usos.registrarGrupo(comando);
    }
}
