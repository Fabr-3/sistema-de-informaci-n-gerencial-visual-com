package com.visual.backend.finanzas.infraestructura.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.comercial.dominio.modelo.EstadoProyecto;
import com.visual.backend.compartido.dominio.Pagina;
import com.visual.backend.compartido.dominio.SolicitudPagina;
import com.visual.backend.finanzas.aplicacion.servicio.ServicioResultados;
import com.visual.backend.finanzas.dominio.modelo.DetalleResultadoProyecto;
import com.visual.backend.finanzas.dominio.modelo.Filtros;
import com.visual.backend.finanzas.dominio.modelo.OrdenResultados;
import com.visual.backend.finanzas.dominio.modelo.ResultadoProyecto;

/** Costos y margen de ganancia por proyecto y por evento. */
@RestController
@RequestMapping("/api/finanzas/resultados/proyectos")
public class ControladorResultados {

    private final ServicioResultados servicio;

    public ControladorResultados(ServicioResultados servicio) {
        this.servicio = servicio;
    }

    /** Proyectos cuyo periodo se superpone con [desde, hasta]; el orden por margen compara su rentabilidad. */
    @GetMapping
    @PreAuthorize(ReglasAccesoFinanzas.VER)
    public Pagina<ResultadoProyecto> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) EstadoProyecto estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer idCliente,
            @RequestParam(defaultValue = "FECHA_DESC") OrdenResultados orden,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return servicio.listarProyectos(new Filtros.ResultadosProyectos(texto, estado == null ? null : estado.name(),
                desde, hasta, idCliente, orden), new SolicitudPagina(pagina, tamano));
    }

    @GetMapping("/{id}")
    @PreAuthorize(ReglasAccesoFinanzas.VER)
    public DetalleResultadoProyecto detalle(@PathVariable Integer id) {
        return servicio.detalleProyecto(id);
    }
}
