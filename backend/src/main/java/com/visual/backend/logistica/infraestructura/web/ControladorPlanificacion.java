package com.visual.backend.logistica.infraestructura.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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

import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.logistica.aplicacion.dto.ComandoAsignacion;
import com.visual.backend.logistica.aplicacion.dto.ComandoJornada;
import com.visual.backend.logistica.aplicacion.dto.ComandoTransporte;
import com.visual.backend.logistica.aplicacion.dto.PersonalDisponible;
import com.visual.backend.logistica.aplicacion.dto.VistaAsignacion;
import com.visual.backend.logistica.aplicacion.dto.VistaPlanEvento;
import com.visual.backend.logistica.aplicacion.servicio.ServicioPersonal;
import com.visual.backend.logistica.aplicacion.servicio.ServicioPlanificacion;
import com.visual.backend.logistica.dominio.modelo.Filtros;
import com.visual.backend.logistica.dominio.modelo.Periodo;
import com.visual.backend.logistica.dominio.modelo.ResumenLogisticoEvento;

import jakarta.validation.Valid;

/** Planificacion logistica por evento: transporte, jornadas y asignacion de personal. */
@RestController
@RequestMapping("/api/logistica/eventos")
public class ControladorPlanificacion {

    private final ServicioPlanificacion planificacion;
    private final ServicioPersonal personal;

    public ControladorPlanificacion(ServicioPlanificacion planificacion, ServicioPersonal personal) {
        this.planificacion = planificacion;
        this.personal = personal;
    }

    /** Eventos entre dos fechas (ambas incluidas) con el avance de su planificacion. */
    @GetMapping
    @PreAuthorize(ReglasAccesoLogistica.VER)
    public Pagina<ResumenLogisticoEvento> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return planificacion.listarEventos(new Filtros.Eventos(desde == null ? null : desde.atStartOfDay(),
                hasta == null ? null : hasta.plusDays(1).atStartOfDay(), texto), new SolicitudPagina(pagina, tamano));
    }

    @GetMapping("/{idEvento}")
    @PreAuthorize(ReglasAccesoLogistica.VER)
    public VistaPlanEvento obtener(@PathVariable Integer idEvento) {
        return planificacion.detalle(idEvento);
    }

    @PutMapping("/{idEvento}/transporte")
    @PreAuthorize(ReglasAccesoLogistica.GESTIONAR)
    public VistaPlanEvento guardarTransporte(@PathVariable Integer idEvento,
                                             @Valid @RequestBody ComandoTransporte comando) {
        return planificacion.guardarTransporte(idEvento, comando.detallesTransporte());
    }

    @PostMapping("/{idEvento}/jornadas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoLogistica.GESTIONAR)
    public VistaPlanEvento agregarJornada(@PathVariable Integer idEvento, @Valid @RequestBody ComandoJornada comando) {
        return planificacion.agregarJornada(idEvento, comando);
    }

    @PutMapping("/{idEvento}/jornadas/{idJornada}")
    @PreAuthorize(ReglasAccesoLogistica.GESTIONAR)
    public VistaPlanEvento modificarJornada(@PathVariable Integer idEvento, @PathVariable Integer idJornada,
                                            @Valid @RequestBody ComandoJornada comando) {
        return planificacion.modificarJornada(idEvento, idJornada, comando);
    }

    @DeleteMapping("/{idEvento}/jornadas/{idJornada}")
    @PreAuthorize(ReglasAccesoLogistica.GESTIONAR)
    public VistaPlanEvento quitarJornada(@PathVariable Integer idEvento, @PathVariable Integer idJornada) {
        return planificacion.quitarJornada(idEvento, idJornada);
    }

    @PostMapping("/{idEvento}/asignaciones")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoLogistica.GESTIONAR)
    public VistaAsignacion asignar(@PathVariable Integer idEvento, @Valid @RequestBody ComandoAsignacion comando) {
        return planificacion.asignar(idEvento, comando);
    }

    @PutMapping("/{idEvento}/asignaciones/{idAsignacion}")
    @PreAuthorize(ReglasAccesoLogistica.GESTIONAR)
    public VistaAsignacion modificarAsignacion(@PathVariable Integer idEvento, @PathVariable Integer idAsignacion,
                                               @Valid @RequestBody ComandoAsignacion comando) {
        return planificacion.modificarAsignacion(idEvento, idAsignacion, comando);
    }

    @DeleteMapping("/{idEvento}/asignaciones/{idAsignacion}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ReglasAccesoLogistica.GESTIONAR)
    public void quitarAsignacion(@PathVariable Integer idEvento, @PathVariable Integer idAsignacion) {
        planificacion.quitarAsignacion(idEvento, idAsignacion);
    }

    /** Personal activo con lo que ya tiene asignado en otros eventos durante ese horario. */
    @GetMapping("/{idEvento}/personal-disponible")
    @PreAuthorize(ReglasAccesoLogistica.GESTIONAR)
    public List<PersonalDisponible> disponibles(
            @PathVariable Integer idEvento,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return personal.disponibles(new Periodo(inicio, fin), planificacion.idPlan(idEvento));
    }
}
