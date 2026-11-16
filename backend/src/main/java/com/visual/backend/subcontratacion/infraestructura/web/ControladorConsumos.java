package com.visual.backend.subcontratacion.infraestructura.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.compartido.dominio.Pagina;
import com.visual.backend.compartido.dominio.SolicitudPagina;
import com.visual.backend.subcontratacion.aplicacion.dto.ComandoAnulacion;
import com.visual.backend.subcontratacion.aplicacion.dto.ComandoConsumo;
import com.visual.backend.subcontratacion.aplicacion.dto.VistaConsumo;
import com.visual.backend.subcontratacion.aplicacion.servicio.ServicioConsumos;
import com.visual.backend.subcontratacion.dominio.modelo.EventoAsignado;
import com.visual.backend.subcontratacion.dominio.modelo.Filtros;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/subcontratacion/consumos")
public class ControladorConsumos {

    private final ServicioConsumos servicio;

    public ControladorConsumos(ServicioConsumos servicio) {
        this.servicio = servicio;
    }

    /** El tecnico solo ve los consumos de los eventos donde esta asignado. */
    @GetMapping
    @PreAuthorize(ReglasAccesoSubcontratacion.VER_CONSUMOS)
    public Pagina<VistaConsumo> listar(
            @RequestParam(required = false) Integer idProyecto,
            @RequestParam(required = false) Integer idEvento,
            @RequestParam(required = false) Integer idConsumible,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return servicio.listar(new Filtros.Consumos(idProyecto, idEvento, idConsumible, desde, hasta, null),
                new SolicitudPagina(pagina, tamano));
    }

    @GetMapping("/eventos-asignados")
    @PreAuthorize(ReglasAccesoSubcontratacion.REGISTRAR_CONSUMOS)
    public List<EventoAsignado> eventosAsignados() {
        return servicio.eventosAsignados();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoSubcontratacion.REGISTRAR_CONSUMOS)
    public VistaConsumo registrar(@Valid @RequestBody ComandoConsumo comando) {
        return servicio.registrar(comando);
    }

    @PostMapping("/{id}/anulacion")
    @PreAuthorize(ReglasAccesoSubcontratacion.REGISTRAR_CONSUMOS)
    public VistaConsumo anular(@PathVariable Integer id, @Valid @RequestBody ComandoAnulacion comando) {
        return servicio.anular(id, comando.motivo());
    }
}
