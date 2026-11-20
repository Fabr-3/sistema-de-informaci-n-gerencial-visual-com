package com.visual.backend.logistica.infraestructura.web;

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

import com.visual.backend.logistica.aplicacion.dto.ComandoLiberacion;
import com.visual.backend.logistica.aplicacion.dto.ComandoReserva;
import com.visual.backend.logistica.aplicacion.dto.ComandoReservaAutomatica;
import com.visual.backend.logistica.aplicacion.dto.VistaReserva;
import com.visual.backend.logistica.aplicacion.servicio.ServicioReservas;
import com.visual.backend.logistica.dominio.modelo.ActivoReservable;
import com.visual.backend.logistica.dominio.modelo.DisponibilidadModelo;
import com.visual.backend.logistica.dominio.modelo.Filtros;

import jakarta.validation.Valid;

/** Reserva de equipos para el plan logistico de un evento. */
@RestController
@RequestMapping("/api/logistica/eventos/{idEvento}")
public class ControladorReservas {

    private final ServicioReservas reservas;

    public ControladorReservas(ServicioReservas reservas) {
        this.reservas = reservas;
    }

    @GetMapping("/reservas")
    @PreAuthorize(ReglasAccesoLogistica.VER)
    public List<VistaReserva> listar(@PathVariable Integer idEvento) {
        return reservas.listar(idEvento);
    }

    /** Equipos por unidad libres durante el periodo de la reserva del evento. */
    @GetMapping("/activos-disponibles")
    @PreAuthorize(ReglasAccesoLogistica.GESTIONAR)
    public List<ActivoReservable> disponibles(@PathVariable Integer idEvento,
                                              @RequestParam(required = false) String texto,
                                              @RequestParam(required = false) Integer idCategoria) {
        return reservas.disponibles(idEvento, new Filtros.ActivosReservables(texto, idCategoria));
    }

    @PostMapping("/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoLogistica.GESTIONAR)
    public List<VistaReserva> reservar(@PathVariable Integer idEvento, @Valid @RequestBody ComandoReserva comando) {
        return reservas.reservar(idEvento, comando);
    }

    /** Unidades libres por modelo, para reservar por cantidades. */
    @GetMapping("/disponibilidad-modelos")
    @PreAuthorize(ReglasAccesoLogistica.GESTIONAR)
    public List<DisponibilidadModelo> disponibilidadPorModelo(@PathVariable Integer idEvento) {
        return reservas.disponibilidadPorModelo(idEvento);
    }

    /** Reserva por cantidades; 409 con los faltantes si algun modelo no alcanza. */
    @PostMapping("/reservas/automatica")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoLogistica.GESTIONAR)
    public List<VistaReserva> reservarAutomaticamente(@PathVariable Integer idEvento,
                                                      @Valid @RequestBody ComandoReservaAutomatica comando) {
        return reservas.reservarAutomaticamente(idEvento, comando);
    }

    @PostMapping("/reservas/{idReserva}/liberacion")
    @PreAuthorize(ReglasAccesoLogistica.GESTIONAR)
    public VistaReserva liberar(@PathVariable Integer idEvento, @PathVariable Integer idReserva,
                                @Valid @RequestBody ComandoLiberacion comando) {
        return reservas.liberar(idEvento, idReserva, comando.motivo());
    }
}
