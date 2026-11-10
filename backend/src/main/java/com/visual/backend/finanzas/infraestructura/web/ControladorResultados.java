package com.visual.backend.finanzas.infraestructura.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.comercial.dominio.modelo.EstadoProyecto;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.finanzas.aplicacion.servicio.ServicioResultados;
import com.visual.backend.finanzas.dominio.modelo.DetalleResultadoProyecto;
import com.visual.backend.finanzas.dominio.modelo.Filtros;
import com.visual.backend.finanzas.dominio.modelo.ResultadoProyecto;

/** Costos y margen de ganancia por proyecto y por evento. */
@RestController
@RequestMapping("/api/finanzas/resultados/proyectos")
public class ControladorResultados {

    private final ServicioResultados servicio;

    public ControladorResultados(ServicioResultados servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(ReglasAccesoFinanzas.VER)
    public Pagina<ResultadoProyecto> listar(@RequestParam(required = false) String texto,
                                            @RequestParam(required = false) EstadoProyecto estado,
                                            @RequestParam(defaultValue = "0") int pagina,
                                            @RequestParam(defaultValue = "20") int tamano) {
        return servicio.listarProyectos(new Filtros.ResultadosProyectos(texto, estado == null ? null : estado.name()),
                new SolicitudPagina(pagina, tamano));
    }

    @GetMapping("/{id}")
    @PreAuthorize(ReglasAccesoFinanzas.VER)
    public DetalleResultadoProyecto detalle(@PathVariable Integer id) {
        return servicio.detalleProyecto(id);
    }
}
