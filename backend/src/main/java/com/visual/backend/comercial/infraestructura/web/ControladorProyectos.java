package com.visual.backend.comercial.infraestructura.web;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.visual.backend.comercial.aplicacion.dto.ComandoContrato;
import com.visual.backend.comercial.aplicacion.dto.ComandoEstadoEvento;
import com.visual.backend.comercial.aplicacion.dto.ComandoEstadoProyecto;
import com.visual.backend.comercial.aplicacion.dto.ComandoEvento;
import com.visual.backend.comercial.aplicacion.dto.ComandoProyecto;
import com.visual.backend.comercial.aplicacion.dto.VistaDetalleProyecto;
import com.visual.backend.comercial.aplicacion.dto.VistaProyecto;
import com.visual.backend.comercial.aplicacion.servicio.ServicioContratos;
import com.visual.backend.comercial.aplicacion.servicio.ServicioEventos;
import com.visual.backend.comercial.aplicacion.servicio.ServicioProyectos;
import com.visual.backend.comercial.dominio.modelo.Contrato;
import com.visual.backend.comercial.dominio.modelo.EstadoProyecto;
import com.visual.backend.comercial.dominio.modelo.Evento;
import com.visual.backend.comercial.dominio.modelo.Filtros;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;

import jakarta.validation.Valid;

/** Proyectos con su cronograma de eventos y sus contratos. */
@RestController
@RequestMapping("/api/comercial/proyectos")
public class ControladorProyectos {

    private static final String PARAMETRO_ARCHIVO = "archivo";

    private final ServicioProyectos proyectos;
    private final ServicioEventos eventos;
    private final ServicioContratos contratos;

    public ControladorProyectos(ServicioProyectos proyectos, ServicioEventos eventos, ServicioContratos contratos) {
        this.proyectos = proyectos;
        this.eventos = eventos;
        this.contratos = contratos;
    }

    @GetMapping
    @PreAuthorize(ReglasAccesoComercial.VER)
    public Pagina<VistaProyecto> listar(@RequestParam(required = false) String texto,
                                        @RequestParam(required = false) EstadoProyecto estado,
                                        @RequestParam(required = false) Integer idCliente,
                                        @RequestParam(defaultValue = "0") int pagina,
                                        @RequestParam(defaultValue = "20") int tamano) {
        return proyectos.listar(new Filtros.Proyectos(texto, estado, idCliente), new SolicitudPagina(pagina, tamano));
    }

    @GetMapping("/{id}")
    @PreAuthorize(ReglasAccesoComercial.VER)
    public VistaDetalleProyecto obtener(@PathVariable Integer id) {
        return proyectos.obtener(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize(ReglasAccesoComercial.GESTIONAR)
    public VistaProyecto actualizar(@PathVariable Integer id, @Valid @RequestBody ComandoProyecto comando) {
        return proyectos.actualizar(id, comando);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize(ReglasAccesoComercial.GESTIONAR)
    public VistaProyecto cambiarEstado(@PathVariable Integer id, @Valid @RequestBody ComandoEstadoProyecto comando) {
        return proyectos.cambiarEstado(id, comando.estado());
    }

    @PostMapping("/{id}/eventos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoComercial.GESTIONAR)
    public Evento crearEvento(@PathVariable Integer id, @Valid @RequestBody ComandoEvento comando) {
        return eventos.crear(id, comando);
    }

    @PutMapping("/{id}/eventos/{idEvento}")
    @PreAuthorize(ReglasAccesoComercial.GESTIONAR)
    public Evento actualizarEvento(@PathVariable Integer id, @PathVariable Integer idEvento,
                                   @Valid @RequestBody ComandoEvento comando) {
        return eventos.actualizar(id, idEvento, comando);
    }

    @PatchMapping("/{id}/eventos/{idEvento}/estado")
    @PreAuthorize(ReglasAccesoComercial.GESTIONAR)
    public Evento cambiarEstadoEvento(@PathVariable Integer id, @PathVariable Integer idEvento,
                                      @Valid @RequestBody ComandoEstadoEvento comando) {
        return eventos.cambiarEstado(id, idEvento, comando.estado());
    }

    @PostMapping("/{id}/contratos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoComercial.GESTIONAR)
    public Contrato crearContrato(@PathVariable Integer id, @Valid @RequestBody ComandoContrato comando) {
        return contratos.crear(id, comando);
    }

    @PutMapping("/{id}/contratos/{idContrato}")
    @PreAuthorize(ReglasAccesoComercial.GESTIONAR)
    public Contrato actualizarContrato(@PathVariable Integer id, @PathVariable Integer idContrato,
                                       @Valid @RequestBody ComandoContrato comando) {
        return contratos.actualizar(id, idContrato, comando);
    }

    @GetMapping("/{id}/contratos/{idContrato}/pdf")
    @PreAuthorize(ReglasAccesoComercial.VER)
    public ResponseEntity<byte[]> descargarContrato(@PathVariable Integer id, @PathVariable Integer idContrato) {
        return RespuestaPdf.de(contratos.pdf(id, idContrato));
    }

    @PutMapping(path = "/{id}/contratos/{idContrato}/firmado", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(ReglasAccesoComercial.GESTIONAR)
    public Contrato adjuntarFirmado(@PathVariable Integer id, @PathVariable Integer idContrato,
                                    @RequestParam(PARAMETRO_ARCHIVO) MultipartFile archivo) throws IOException {
        return contratos.adjuntarFirmado(id, idContrato, archivo.getBytes());
    }

    @GetMapping("/{id}/contratos/{idContrato}/firmado")
    @PreAuthorize(ReglasAccesoComercial.VER)
    public ResponseEntity<byte[]> descargarFirmado(@PathVariable Integer id, @PathVariable Integer idContrato) {
        return RespuestaPdf.de(contratos.firmado(id, idContrato));
    }
}
