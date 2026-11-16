package com.visual.backend.gerencial.infraestructura.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.gerencial.aplicacion.dto.ComandoAtencionAlerta;
import com.visual.backend.gerencial.aplicacion.servicio.ServicioAlertasActivos;
import com.visual.backend.gerencial.dominio.modelo.Alerta;
import com.visual.backend.gerencial.dominio.modelo.EstadoAlerta;
import com.visual.backend.gerencial.dominio.modelo.FiltroAlertas;
import com.visual.backend.gerencial.dominio.modelo.Severidad;
import com.visual.backend.gerencial.dominio.modelo.TipoAlerta;

import jakarta.validation.Valid;

/** Alertas de activos, reservas y consumibles, y su atencion por el gerente. */
@RestController
@RequestMapping("/api/gerencial/alertas")
public class ControladorAlertas {

    private final ServicioAlertasActivos servicio;

    public ControladorAlertas(ServicioAlertasActivos servicio) {
        this.servicio = servicio;
    }

    /** Sincroniza con la situacion actual y lista las alertas del estado pedido (por defecto, pendientes). */
    @GetMapping
    @PreAuthorize(ReglasAccesoGerencial.VER)
    public List<Alerta> listar(@RequestParam(defaultValue = "PENDIENTE") EstadoAlerta estado,
                               @RequestParam(required = false) TipoAlerta tipo,
                               @RequestParam(required = false) Severidad severidad) {
        return servicio.listar(new FiltroAlertas(estado, tipo, severidad));
    }

    @PostMapping("/{id}/atencion")
    @PreAuthorize(ReglasAccesoGerencial.VER)
    public Alerta atender(@PathVariable Long id, @Valid @RequestBody ComandoAtencionAlerta comando) {
        return servicio.atender(id, comando.comentario());
    }
}
