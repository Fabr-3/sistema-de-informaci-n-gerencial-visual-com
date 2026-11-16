package com.visual.backend.subcontratacion.infraestructura.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.compartido.dominio.Pagina;
import com.visual.backend.compartido.dominio.SolicitudPagina;
import com.visual.backend.subcontratacion.aplicacion.dto.ComandoAjusteConsumible;
import com.visual.backend.subcontratacion.aplicacion.dto.ComandoConsumible;
import com.visual.backend.subcontratacion.aplicacion.dto.ComandoIngresoConsumible;
import com.visual.backend.subcontratacion.aplicacion.dto.VistaConsumible;
import com.visual.backend.subcontratacion.aplicacion.servicio.ServicioConsumibles;
import com.visual.backend.subcontratacion.dominio.modelo.Filtros;
import com.visual.backend.subcontratacion.dominio.modelo.MovimientoConsumible;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/subcontratacion/consumibles")
public class ControladorConsumibles {

    private final ServicioConsumibles servicio;

    public ControladorConsumibles(ServicioConsumibles servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(ReglasAccesoSubcontratacion.VER_CONSUMOS)
    public Pagina<VistaConsumible> listar(@RequestParam(required = false) String texto,
                                          @RequestParam(required = false) Boolean activo,
                                          @RequestParam(defaultValue = "false") boolean soloStockBajo,
                                          @RequestParam(defaultValue = "0") int pagina,
                                          @RequestParam(defaultValue = "20") int tamano) {
        return servicio.listar(new Filtros.Consumibles(texto, activo, soloStockBajo),
                new SolicitudPagina(pagina, tamano));
    }

    @GetMapping("/{id}")
    @PreAuthorize(ReglasAccesoSubcontratacion.VER_CONSUMOS)
    public VistaConsumible obtener(@PathVariable Integer id) {
        return servicio.obtener(id);
    }

    @GetMapping("/{id}/movimientos")
    @PreAuthorize(ReglasAccesoSubcontratacion.VER)
    public Pagina<MovimientoConsumible> movimientos(@PathVariable Integer id,
                                                    @RequestParam(defaultValue = "0") int pagina,
                                                    @RequestParam(defaultValue = "20") int tamano) {
        return servicio.movimientos(id, new SolicitudPagina(pagina, tamano));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoSubcontratacion.GESTIONAR)
    public VistaConsumible registrar(@Valid @RequestBody ComandoConsumible comando) {
        return servicio.registrar(comando);
    }

    @PutMapping("/{id}")
    @PreAuthorize(ReglasAccesoSubcontratacion.GESTIONAR)
    public VistaConsumible actualizar(@PathVariable Integer id, @Valid @RequestBody ComandoConsumible comando) {
        return servicio.actualizar(id, comando);
    }

    @PostMapping("/{id}/ingresos")
    @PreAuthorize(ReglasAccesoSubcontratacion.GESTIONAR)
    public VistaConsumible ingresar(@PathVariable Integer id, @Valid @RequestBody ComandoIngresoConsumible comando) {
        return servicio.ingresar(id, comando);
    }

    @PostMapping("/{id}/ajustes")
    @PreAuthorize(ReglasAccesoSubcontratacion.GESTIONAR)
    public VistaConsumible ajustar(@PathVariable Integer id, @Valid @RequestBody ComandoAjusteConsumible comando) {
        return servicio.ajustar(id, comando);
    }
}
