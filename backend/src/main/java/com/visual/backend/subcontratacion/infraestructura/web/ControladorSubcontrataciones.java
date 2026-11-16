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

import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.compartido.dominio.Pagina;
import com.visual.backend.compartido.dominio.SolicitudPagina;
import com.visual.backend.subcontratacion.aplicacion.dto.ComandoAnulacion;
import com.visual.backend.subcontratacion.aplicacion.dto.ComandoSubcontratacion;
import com.visual.backend.subcontratacion.aplicacion.servicio.ReferenciasImputacion;
import com.visual.backend.subcontratacion.aplicacion.servicio.ServicioSubcontrataciones;
import com.visual.backend.subcontratacion.dominio.modelo.Filtros;
import com.visual.backend.subcontratacion.dominio.modelo.Subcontratacion;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/subcontratacion")
public class ControladorSubcontrataciones {

    private final ServicioSubcontrataciones servicio;
    private final ReferenciasImputacion referencias;

    public ControladorSubcontrataciones(ServicioSubcontrataciones servicio, ReferenciasImputacion referencias) {
        this.servicio = servicio;
        this.referencias = referencias;
    }

    /** Subcontrataciones con fecha entre desde y hasta (ambas incluidas). */
    @GetMapping("/subcontrataciones")
    @PreAuthorize(ReglasAccesoSubcontratacion.VER)
    public Pagina<Subcontratacion> listar(
            @RequestParam(required = false) Integer idProyecto,
            @RequestParam(required = false) Integer idEvento,
            @RequestParam(required = false) Integer idProveedor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return servicio.listar(new Filtros.Subcontrataciones(idProyecto, idEvento, idProveedor, desde, hasta),
                new SolicitudPagina(pagina, tamano));
    }

    @GetMapping("/subcontrataciones/{id}")
    @PreAuthorize(ReglasAccesoSubcontratacion.VER)
    public Subcontratacion obtener(@PathVariable Integer id) {
        return servicio.obtener(id);
    }

    @PostMapping("/subcontrataciones")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoSubcontratacion.GESTIONAR)
    public Subcontratacion registrar(@Valid @RequestBody ComandoSubcontratacion comando) {
        return servicio.registrar(comando);
    }

    @PostMapping("/subcontrataciones/{id}/anulacion")
    @PreAuthorize(ReglasAccesoSubcontratacion.GESTIONAR)
    public Subcontratacion anular(@PathVariable Integer id, @Valid @RequestBody ComandoAnulacion comando) {
        return servicio.anular(id, comando.motivo());
    }

    /** Servicios del proveedor (con su id) para elegir el subcontratado. */
    @GetMapping("/proveedores/{idProveedor}/servicios")
    @PreAuthorize(ReglasAccesoSubcontratacion.VER)
    public List<ElementoCatalogo> servicios(@PathVariable Integer idProveedor) {
        return referencias.servicios(idProveedor);
    }
}
