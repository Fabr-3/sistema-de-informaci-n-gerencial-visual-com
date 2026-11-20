package com.visual.backend.subcontratacion.dominio.modelo;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/**
 * Recibo o factura de un gasto (foto o PDF). El archivo se guarda con un
 * nombre generado por el sistema; se conserva el nombre original para la
 * descarga. Se valida el tipo por su firma, no solo por lo que declara el
 * navegador.
 */
public record Comprobante(String archivo, String nombreOriginal, String tipoContenido) {

    public static final long TAMANO_MAXIMO = 5L * 1024 * 1024;
    static final int LONGITUD_MAXIMA_NOMBRE = 255;

    /** Tipo de contenido -> extension y firma (primeros bytes) del archivo. */
    private static final Map<String, Formato> FORMATOS = Map.of(
            "application/pdf", new Formato("pdf", new byte[] {'%', 'P', 'D', 'F'}),
            "image/jpeg", new Formato("jpg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
            "image/png", new Formato("png", new byte[] {(byte) 0x89, 'P', 'N', 'G'}),
            "image/webp", new Formato("webp", new byte[] {'R', 'I', 'F', 'F'}));

    /** Valida el archivo recibido y le asigna un nombre unico para guardarlo. */
    public static Comprobante recibido(String nombreOriginal, String tipoContenido, byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            throw new ReglaNegocioExcepcion("Adjunte el comprobante del gasto (foto o PDF)");
        }
        if (contenido.length > TAMANO_MAXIMO) {
            throw new ReglaNegocioExcepcion("El comprobante no puede superar los 5 MB");
        }
        Formato formato = tipoContenido == null ? null : FORMATOS.get(tipoContenido);
        if (formato == null || !formato.coincide(contenido)) {
            throw new ReglaNegocioExcepcion("El comprobante debe ser una imagen (JPG, PNG o WEBP) o un PDF");
        }
        String nombre = nombreOriginal == null || nombreOriginal.isBlank() ? "comprobante." + formato.extension()
                : nombreOriginal.strip();
        if (nombre.length() > LONGITUD_MAXIMA_NOMBRE) {
            nombre = nombre.substring(nombre.length() - LONGITUD_MAXIMA_NOMBRE);
        }
        return new Comprobante("gasto-" + UUID.randomUUID() + "." + formato.extension(), nombre, tipoContenido);
    }

    private record Formato(String extension, byte[] firma) {

        boolean coincide(byte[] contenido) {
            return contenido.length >= firma.length
                    && Arrays.equals(Arrays.copyOf(contenido, firma.length), firma);
        }
    }
}
