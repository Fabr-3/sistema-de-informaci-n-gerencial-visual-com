package com.visual.backend.comercial.infraestructura.web;

import org.springframework.http.HttpStatus;
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

import com.visual.backend.comercial.aplicacion.dto.ComandoCliente;
import com.visual.backend.comercial.aplicacion.dto.VistaCliente;
import com.visual.backend.comercial.aplicacion.servicio.ServicioClientes;
import com.visual.backend.comercial.dominio.modelo.Filtros;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/comercial/clientes")
public class ControladorClientes {

    private final ServicioClientes servicio;

    public ControladorClientes(ServicioClientes servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(ReglasAccesoComercial.VER)
    public Pagina<VistaCliente> listar(@RequestParam(required = false) String texto,
                                       @RequestParam(required = false) Integer idTipoCliente,
                                       @RequestParam(defaultValue = "0") int pagina,
                                       @RequestParam(defaultValue = "20") int tamano) {
        return servicio.listar(new Filtros.Clientes(texto, idTipoCliente), new SolicitudPagina(pagina, tamano));
    }

    @GetMapping("/{id}")
    @PreAuthorize(ReglasAccesoComercial.VER)
    public VistaCliente obtener(@PathVariable Integer id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoComercial.GESTIONAR)
    public VistaCliente crear(@Valid @RequestBody ComandoCliente comando) {
        return servicio.crear(comando);
    }

    @PutMapping("/{id}")
    @PreAuthorize(ReglasAccesoComercial.GESTIONAR)
    public VistaCliente actualizar(@PathVariable Integer id, @Valid @RequestBody ComandoCliente comando) {
        return servicio.actualizar(id, comando);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ReglasAccesoComercial.GESTIONAR)
    public void eliminar(@PathVariable Integer id) {
        servicio.eliminar(id);
    }
}
