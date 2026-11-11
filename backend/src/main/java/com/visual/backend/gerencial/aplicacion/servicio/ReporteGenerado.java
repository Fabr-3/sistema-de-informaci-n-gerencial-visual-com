package com.visual.backend.gerencial.aplicacion.servicio;

/** Reporte en PDF listo para descargar. */
public record ReporteGenerado(String nombreArchivo, byte[] contenido) {
}
