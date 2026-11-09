package com.visual.backend.comercial.infraestructura.archivos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.visual.backend.comercial.dominio.puerto.AlmacenDocumentos;
import com.visual.backend.comercial.infraestructura.configuracion.PropiedadesDocumentos;

/**
 * Guarda los documentos en una carpeta local. Solo admite nombres simples
 * generados por el sistema, de modo que nunca se escribe fuera de la carpeta.
 */
@Component
public class AlmacenDocumentosDisco implements AlmacenDocumentos {

    private static final Pattern NOMBRE_VALIDO = Pattern.compile("[A-Za-z0-9-]{1,60}\\.pdf");
    private static final String SUFIJO_TEMPORAL = ".tmp";

    private final PropiedadesDocumentos propiedades;

    public AlmacenDocumentosDisco(PropiedadesDocumentos propiedades) {
        this.propiedades = propiedades;
    }

    @Override
    public void guardar(String nombre, byte[] contenido) {
        Path destino = ruta(nombre);
        try {
            Files.createDirectories(destino.getParent());
            // Se escribe primero en un temporal para no dejar un documento a medias
            Path temporal = Files.createTempFile(destino.getParent(), nombre, SUFIJO_TEMPORAL);
            Files.write(temporal, contenido);
            Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException excepcion) {
            throw new UncheckedIOException("No se pudo guardar el documento " + nombre, excepcion);
        }
    }

    @Override
    public Optional<byte[]> leer(String nombre) {
        Path origen = ruta(nombre);
        if (!Files.isRegularFile(origen)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(origen));
        } catch (IOException excepcion) {
            throw new UncheckedIOException("No se pudo leer el documento " + nombre, excepcion);
        }
    }

    private Path ruta(String nombre) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            throw new IllegalArgumentException("Nombre de documento no permitido");
        }
        Path directorio = Path.of(propiedades.directorioContratos()).toAbsolutePath().normalize();
        return directorio.resolve(nombre);
    }
}
