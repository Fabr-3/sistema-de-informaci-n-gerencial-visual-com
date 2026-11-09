package com.visual.backend.respaldos.dominio;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Copia de seguridad de la base de datos. El nombre del archivo se genera a
 * partir de la fecha y solo se aceptan nombres con ese formato, lo que impide
 * acceder a rutas arbitrarias (path traversal).
 */
public record Respaldo(String nombreArchivo, long tamanoBytes, LocalDateTime fechaCreacion) {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final Pattern FORMATO_NOMBRE = Pattern.compile("^respaldo_\\d{8}_\\d{6}_\\d{3}\\.dump$");
    private static final String PREFIJO = "respaldo_";
    private static final String EXTENSION = ".dump";

    public static String nombreParaFecha(LocalDateTime fecha) {
        return PREFIJO + FORMATO_FECHA.format(fecha) + EXTENSION;
    }

    public static boolean esNombreValido(String nombreArchivo) {
        return nombreArchivo != null && FORMATO_NOMBRE.matcher(nombreArchivo).matches();
    }
}
