package com.visual.backend.subcontratacion.aplicacion.dto;

/** Archivo recibido o devuelto (comprobante): nombre, tipo de contenido y bytes. */
public record ArchivoAdjunto(String nombre, String tipoContenido, byte[] contenido) {
}
