package com.visual.backend.comercial.infraestructura.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.comercial.aplicacion.dto.ComandoCotizacion;
import com.visual.backend.comercial.aplicacion.dto.ComandoMotivo;
import com.visual.backend.comercial.aplicacion.dto.VistaCotizacion;
import com.visual.backend.comercial.aplicacion.servicio.ServicioCotizaciones;
import com.visual.backend.comercial.dominio.modelo.EstadoCotizacion;
import com.visual.backend.comercial.dominio.modelo.Filtros;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/comercial/cotizaciones")
public class ControladorCotizaciones {

    private final ServicioCotizaciones servicio;

    public ControladorCotizaciones(ServicioCotizaciones servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(ReglasAccesoComercial.VER)
    public Pagina<VistaCotizacion> listar(@RequestParam(required = false) String texto,
                                          @RequestParam(required = false) EstadoCotizacion estado,
                                          @RequestParam(required = false) Integer idCliente,
                                          @RequestParam(defaultValue = "0") int pagina,
                                          @RequestParam(defaultValue = "20") int tamano) {
        return servicio.listar(new Filtros.Cotizaciones(texto, estado, idCliente), new SolicitudPagina(pagina, tamano));
    }

    @GetMapping("/{id}")
    @PreAuthorize(ReglasAccesoComercial.VER)
    public VistaCotizacion obtener(@PathVariable Integer id) {
        return servicio.obtener(id);
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize(ReglasAccesoComercial.VER)
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Integer id) {
        return RespuestaPdf.de(servicio.pdf(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoComercial.GESTIONAR)
    public VistaCotizacion crear(@Valid @RequestBody ComandoCotizacion comando) {
        return servicio.crear(comando);
    }

    @PutMapping("/{id}")
    @PreAuthorize(ReglasAccesoComercial.GESTIONAR)
    public VistaCotizacion actualizar(@PathVariable Integer id, @Valid @RequestBody ComandoCotizacion comando) {
        return servicio.actualizar(id, comando);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ReglasAccesoComercial.GESTIONAR)
    public void eliminar(@PathVariable Integer id) {
        servicio.eliminar(id);
    }

    @PostMapping("/{id}/aceptacion")
    @PreAuthorize(ReglasAccesoComercial.APROBAR)
    public VistaCotizacion aceptar(@PathVariable Integer id) {
        return servicio.aceptar(id);
    }

    @PostMapping("/{id}/rechazo")
    @PreAuthorize(ReglasAccesoComercial.APROBAR)
    public VistaCotizacion rechazar(@PathVariable Integer id, @Valid @RequestBody ComandoMotivo comando) {
        return servicio.rechazar(id, comando.motivo());
    }
}
