package com.visual.backend.gerencial.infraestructura.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.gerencial.aplicacion.servicio.ReporteGenerado;
import com.visual.backend.gerencial.aplicacion.servicio.ServicioReportes;
import com.visual.backend.gerencial.dominio.modelo.TipoReporte;

/** Descarga de los reportes operativos en PDF. */
@RestController
@RequestMapping("/api/gerencial/reportes")
public class ControladorReportes {

    private final ServicioReportes servicio;

    public ControladorReportes(ServicioReportes servicio) {
        this.servicio = servicio;
    }

    /** Sin fechas: el mes en curso hasta hoy. */
    @GetMapping("/{tipo}")
    @PreAuthorize(ReglasAccesoGerencial.VER)
    public ResponseEntity<byte[]> generar(@PathVariable TipoReporte tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        ReporteGenerado reporte = servicio.generar(tipo, desde, hasta);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(reporte.nombreArchivo()).build().toString())
                .body(reporte.contenido());
    }
}
