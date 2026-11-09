package com.visual.backend.comercial.infraestructura.web;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.visual.backend.comercial.aplicacion.servicio.DocumentoGenerado;

/** Respuesta HTTP para descargar un documento PDF generado. */
final class RespuestaPdf {

    private RespuestaPdf() {
    }

    static ResponseEntity<byte[]> de(DocumentoGenerado documento) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(documento.nombreArchivo()).build().toString())
                .body(documento.contenido());
    }
}
