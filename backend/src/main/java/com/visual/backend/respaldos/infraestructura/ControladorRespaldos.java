package com.visual.backend.respaldos.infraestructura;

import java.nio.file.Path;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.accesos.dominio.modelo.Permisos;
import com.visual.backend.respaldos.aplicacion.ServicioRespaldos;
import com.visual.backend.respaldos.dominio.Respaldo;

@RestController
@RequestMapping("/api/respaldos")
@PreAuthorize("hasAuthority('" + Permisos.RESPALDOS_GESTIONAR + "')")
public class ControladorRespaldos {

    private final ServicioRespaldos servicio;

    public ControladorRespaldos(ServicioRespaldos servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Respaldo> listar() {
        return servicio.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Respaldo generar() {
        return servicio.generar();
    }

    @GetMapping("/{nombreArchivo}/descarga")
    public ResponseEntity<Resource> descargar(@PathVariable String nombreArchivo) {
        Path ruta = servicio.obtenerParaDescarga(nombreArchivo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombreArchivo).build().toString())
                .body(new FileSystemResource(ruta));
    }
}
