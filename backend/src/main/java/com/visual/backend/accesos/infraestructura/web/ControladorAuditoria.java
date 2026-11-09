package com.visual.backend.accesos.infraestructura.web;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.modelo.FiltroAuditoria;
import com.visual.backend.accesos.dominio.modelo.Permisos;
import com.visual.backend.accesos.dominio.modelo.RegistroAuditoria;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;

@RestController
@RequestMapping("/api/auditoria")
public class ControladorAuditoria {

    private final ServicioAuditoria servicio;

    public ControladorAuditoria(ServicioAuditoria servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.AUDITORIA_VER + "')")
    public Pagina<RegistroAuditoria> consultar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) Integer idUsuario,
            @RequestParam(required = false) AccionAuditoria accion,
            @RequestParam(required = false) String entidad,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return servicio.consultar(new FiltroAuditoria(desde, hasta, idUsuario, accion, entidad),
                new SolicitudPagina(pagina, tamano));
    }
}
