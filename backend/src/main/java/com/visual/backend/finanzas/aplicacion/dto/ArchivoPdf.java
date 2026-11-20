package com.visual.backend.finanzas.aplicacion.dto;

/** PDF generado listo para descargar. */
public record ArchivoPdf(String nombreArchivo, byte[] contenido) {
}
