package com.visual.backend.activos.infraestructura.web;

import java.util.List;

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

import com.visual.backend.activos.aplicacion.dto.ComandoMovimientoLote;
import com.visual.backend.activos.aplicacion.dto.ComandoTrasladoLote;
import com.visual.backend.activos.aplicacion.dto.VistaExistencia;
import com.visual.backend.activos.aplicacion.servicio.ServicioLotes;
import com.visual.backend.activos.dominio.modelo.FiltroExistencias;
import com.visual.backend.activos.dominio.modelo.MovimientoLote;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/activos/lotes")
public class ControladorLotes {

    private final ServicioLotes servicio;

    public ControladorLotes(ServicioLotes servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(ReglasAccesoActivos.VER)
    public List<VistaExistencia> listar(@RequestParam(required = false) String texto,
                                        @RequestParam(required = false) Integer idCategoria,
                                        @RequestParam(required = false) Integer idBodega) {
        return servicio.listar(new FiltroExistencias(texto, idCategoria, idBodega));
    }

    @GetMapping("/{idExistencia}/movimientos")
    @PreAuthorize(ReglasAccesoActivos.VER)
    public Pagina<MovimientoLote> listarMovimientos(@PathVariable Integer idExistencia,
                                                    @RequestParam(defaultValue = "0") int pagina,
                                                    @RequestParam(defaultValue = "20") int tamano) {
        return servicio.listarMovimientos(idExistencia, new SolicitudPagina(pagina, tamano));
    }

    @PostMapping("/movimientos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoActivos.GESTIONAR)
    public VistaExistencia registrarMovimiento(@Valid @RequestBody ComandoMovimientoLote comando) {
        return servicio.registrarMovimiento(comando);
    }

    @PostMapping("/traslados")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoActivos.GESTIONAR)
    public List<VistaExistencia> trasladar(@Valid @RequestBody ComandoTrasladoLote comando) {
        return servicio.trasladar(comando);
    }
}
