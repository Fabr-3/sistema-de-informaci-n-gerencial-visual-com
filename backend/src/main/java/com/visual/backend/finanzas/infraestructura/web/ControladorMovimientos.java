package com.visual.backend.finanzas.infraestructura.web;

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

import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.finanzas.aplicacion.dto.ComandoAnulacion;
import com.visual.backend.finanzas.aplicacion.dto.ComandoMovimiento;
import com.visual.backend.finanzas.aplicacion.dto.ListadoMovimientos;
import com.visual.backend.finanzas.aplicacion.servicio.ServicioMovimientos;
import com.visual.backend.finanzas.dominio.modelo.EstadoMovimiento;
import com.visual.backend.finanzas.dominio.modelo.Filtros;
import com.visual.backend.finanzas.dominio.modelo.MovimientoFinanciero;
import com.visual.backend.finanzas.dominio.modelo.Naturaleza;
import com.visual.backend.finanzas.dominio.modelo.TipoMovimiento;

import jakarta.validation.Valid;

/** Ingresos y egresos: cobros, ventas, alquileres, compras, subcontrataciones y gastos. */
@RestController
@RequestMapping("/api/finanzas")
public class ControladorMovimientos {

    private final ServicioMovimientos servicio;

    public ControladorMovimientos(ServicioMovimientos servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/tipos-movimiento")
    @PreAuthorize(ReglasAccesoFinanzas.VER)
    public List<TipoMovimiento> tipos() {
        return servicio.tipos();
    }

    @GetMapping("/movimientos")
    @PreAuthorize(ReglasAccesoFinanzas.VER)
    public ListadoMovimientos listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Naturaleza naturaleza,
            @RequestParam(required = false) Integer idTipo,
            @RequestParam(required = false) Integer idProyecto,
            @RequestParam(required = false) Integer idEvento,
            @RequestParam(required = false) Integer idProveedor,
            @RequestParam(required = false) EstadoMovimiento estado,
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return servicio.listar(new Filtros.Movimientos(desde, hasta, naturaleza, idTipo, idProyecto, idEvento,
                idProveedor, estado, texto), new SolicitudPagina(pagina, tamano));
    }

    @GetMapping("/movimientos/{id}")
    @PreAuthorize(ReglasAccesoFinanzas.VER)
    public MovimientoFinanciero obtener(@PathVariable Integer id) {
        return servicio.obtener(id);
    }

    @PostMapping("/movimientos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoFinanzas.GESTIONAR)
    public MovimientoFinanciero registrar(@Valid @RequestBody ComandoMovimiento comando) {
        return servicio.registrar(comando);
    }

    @PostMapping("/movimientos/{id}/anulacion")
    @PreAuthorize(ReglasAccesoFinanzas.GESTIONAR)
    public MovimientoFinanciero anular(@PathVariable Integer id, @Valid @RequestBody ComandoAnulacion comando) {
        return servicio.anular(id, comando.motivo());
    }
}
