package com.visual.backend.subcontratacion.infraestructura.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.visual.backend.compartido.dominio.Pagina;
import com.visual.backend.compartido.dominio.SolicitudPagina;
import com.visual.backend.subcontratacion.aplicacion.dto.ArchivoAdjunto;
import com.visual.backend.subcontratacion.aplicacion.dto.ComandoAnulacion;
import com.visual.backend.subcontratacion.aplicacion.dto.ComandoGasto;
import com.visual.backend.subcontratacion.aplicacion.dto.VistaGasto;
import com.visual.backend.subcontratacion.aplicacion.servicio.ServicioGastosEvento;
import com.visual.backend.subcontratacion.dominio.modelo.Filtros;

import jakarta.validation.Valid;

/** Gastos del evento con comprobante (foto o PDF), registrados por el personal tecnico. */
@RestController
@RequestMapping("/api/subcontratacion/gastos-evento")
public class ControladorGastosEvento {

    private final ServicioGastosEvento servicio;

    public ControladorGastosEvento(ServicioGastosEvento servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(ReglasAccesoSubcontratacion.VER_CONSUMOS)
    public Pagina<VistaGasto> listar(
            @RequestParam(required = false) Integer idProyecto,
            @RequestParam(required = false) Integer idEvento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return servicio.listar(new Filtros.Gastos(idProyecto, idEvento, desde, hasta, null),
                new SolicitudPagina(pagina, tamano));
    }

    /** Parte "datos" (JSON) con el gasto y parte "archivo" con el comprobante. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoSubcontratacion.REGISTRAR_CONSUMOS)
    public VistaGasto registrar(@Valid @RequestPart("datos") ComandoGasto comando,
                                @RequestPart("archivo") MultipartFile archivo) throws IOException {
        return servicio.registrar(comando, new ArchivoAdjunto(archivo.getOriginalFilename(),
                archivo.getContentType(), archivo.getBytes()));
    }

    /** Se muestra en el navegador (imagen o PDF) con su nombre original. */
    @GetMapping("/{id}/comprobante")
    @PreAuthorize(ReglasAccesoSubcontratacion.VER_CONSUMOS)
    public ResponseEntity<byte[]> comprobante(@PathVariable Integer id) {
        ArchivoAdjunto archivo = servicio.comprobante(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(archivo.nombre(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(archivo.contenido());
    }

    @PostMapping("/{id}/anulacion")
    @PreAuthorize(ReglasAccesoSubcontratacion.REGISTRAR_CONSUMOS)
    public VistaGasto anular(@PathVariable Integer id, @Valid @RequestBody ComandoAnulacion comando) {
        return servicio.anular(id, comando.motivo());
    }
}
