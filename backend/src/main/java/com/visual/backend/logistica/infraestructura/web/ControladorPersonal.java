package com.visual.backend.logistica.infraestructura.web;

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

import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.logistica.aplicacion.dto.ComandoPersonal;
import com.visual.backend.logistica.aplicacion.dto.VistaPersonal;
import com.visual.backend.logistica.aplicacion.servicio.ServicioPersonal;
import com.visual.backend.logistica.dominio.modelo.Filtros;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/logistica/personal")
public class ControladorPersonal {

    private final ServicioPersonal servicio;

    public ControladorPersonal(ServicioPersonal servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(ReglasAccesoLogistica.VER)
    public Pagina<VistaPersonal> listar(@RequestParam(required = false) String texto,
                                        @RequestParam(required = false) Boolean activo,
                                        @RequestParam(required = false) Integer idEspecialidad,
                                        @RequestParam(defaultValue = "0") int pagina,
                                        @RequestParam(defaultValue = "20") int tamano) {
        return servicio.listar(new Filtros.Personal(texto, activo, idEspecialidad), new SolicitudPagina(pagina, tamano));
    }

    @GetMapping("/{id}")
    @PreAuthorize(ReglasAccesoLogistica.VER)
    public VistaPersonal obtener(@PathVariable Integer id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoLogistica.GESTIONAR)
    public VistaPersonal crear(@Valid @RequestBody ComandoPersonal comando) {
        return servicio.crear(comando);
    }

    @PutMapping("/{id}")
    @PreAuthorize(ReglasAccesoLogistica.GESTIONAR)
    public VistaPersonal actualizar(@PathVariable Integer id, @Valid @RequestBody ComandoPersonal comando) {
        return servicio.actualizar(id, comando);
    }
}
