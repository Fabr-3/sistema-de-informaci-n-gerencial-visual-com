package com.visual.backend.activos.infraestructura.web;

import java.util.List;

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

import com.visual.backend.activos.aplicacion.dto.ComandoActualizarActivo;
import com.visual.backend.activos.aplicacion.dto.ComandoRegistroActivos;
import com.visual.backend.activos.aplicacion.dto.ComandoBaja;
import com.visual.backend.activos.aplicacion.dto.VistaActivo;
import com.visual.backend.activos.aplicacion.servicio.ServicioActivos;
import com.visual.backend.activos.dominio.modelo.EstadoOperativo;
import com.visual.backend.activos.dominio.modelo.FiltroActivos;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/activos")
public class ControladorActivos {

    private final ServicioActivos servicio;

    public ControladorActivos(ServicioActivos servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(ReglasAccesoActivos.VER)
    public Pagina<VistaActivo> listar(@RequestParam(required = false) String texto,
                                      @RequestParam(required = false) Integer idCategoria,
                                      @RequestParam(required = false) Integer idModelo,
                                      @RequestParam(required = false) Integer idBodega,
                                      @RequestParam(required = false) EstadoOperativo estado,
                                      @RequestParam(defaultValue = "false") boolean soloAlertas,
                                      @RequestParam(defaultValue = "0") int pagina,
                                      @RequestParam(defaultValue = "20") int tamano) {
        return servicio.listar(new FiltroActivos(texto, idCategoria, idModelo, idBodega, estado, soloAlertas),
                new SolicitudPagina(pagina, tamano));
    }

    @GetMapping("/alertas-mantenimiento")
    @PreAuthorize(ReglasAccesoActivos.VER)
    public List<VistaActivo> listarAlertasMantenimiento() {
        return servicio.listarAlertasMantenimiento();
    }

    @GetMapping("/{id}")
    @PreAuthorize(ReglasAccesoActivos.VER)
    public VistaActivo obtener(@PathVariable Integer id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoActivos.GESTIONAR)
    public List<VistaActivo> registrar(@Valid @RequestBody ComandoRegistroActivos comando) {
        return servicio.registrar(comando);
    }

    @PutMapping("/{id}")
    @PreAuthorize(ReglasAccesoActivos.GESTIONAR)
    public VistaActivo actualizar(@PathVariable Integer id, @Valid @RequestBody ComandoActualizarActivo comando) {
        return servicio.actualizar(id, comando);
    }

    @PostMapping("/{id}/baja")
    @PreAuthorize(ReglasAccesoActivos.GESTIONAR)
    public VistaActivo darDeBaja(@PathVariable Integer id, @Valid @RequestBody ComandoBaja comando) {
        return servicio.darDeBaja(id, comando.motivo());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ReglasAccesoActivos.GESTIONAR)
    public void eliminar(@PathVariable Integer id) {
        servicio.eliminar(id);
    }
}
