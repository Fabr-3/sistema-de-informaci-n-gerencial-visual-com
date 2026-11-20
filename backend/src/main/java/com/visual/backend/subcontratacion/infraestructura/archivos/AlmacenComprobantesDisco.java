package com.visual.backend.subcontratacion.infraestructura.archivos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.visual.backend.subcontratacion.dominio.puerto.AlmacenComprobantes;
import com.visual.backend.subcontratacion.infraestructura.configuracion.PropiedadesComprobantes;

/**
 * Guarda los comprobantes en una carpeta local. Solo admite los nombres que
 * genera el sistema, de modo que nunca se escribe ni se lee fuera de ella.
 */
@Component
public class AlmacenComprobantesDisco implements AlmacenComprobantes {

    private static final Pattern NOMBRE_VALIDO = Pattern.compile("gasto-[a-f0-9-]{36}\\.(pdf|jpg|png|webp)");
    private static final String SUFIJO_TEMPORAL = ".tmp";

    private final PropiedadesComprobantes propiedades;

    public AlmacenComprobantesDisco(PropiedadesComprobantes propiedades) {
        this.propiedades = propiedades;
    }

    @Override
    public void guardar(String archivo, byte[] contenido) {
        Path destino = ruta(archivo);
        try {
            Files.createDirectories(destino.getParent());
            // Se escribe primero en un temporal para no dejar un archivo a medias
            Path temporal = Files.createTempFile(destino.getParent(), archivo, SUFIJO_TEMPORAL);
            Files.write(temporal, contenido);
            Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException excepcion) {
            throw new UncheckedIOException("No se pudo guardar el comprobante " + archivo, excepcion);
        }
    }

    @Override
    public Optional<byte[]> leer(String archivo) {
        Path origen = ruta(archivo);
        if (!Files.isRegularFile(origen)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(origen));
        } catch (IOException excepcion) {
            throw new UncheckedIOException("No se pudo leer el comprobante " + archivo, excepcion);
        }
    }

    @Override
    public void eliminar(String archivo) {
        try {
            Files.deleteIfExists(ruta(archivo));
        } catch (IOException excepcion) {
            throw new UncheckedIOException("No se pudo eliminar el comprobante " + archivo, excepcion);
        }
    }

    private Path ruta(String archivo) {
        if (archivo == null || !NOMBRE_VALIDO.matcher(archivo).matches()) {
            throw new IllegalArgumentException("Nombre de comprobante no permitido");
        }
        return Path.of(propiedades.directorioComprobantes()).toAbsolutePath().normalize().resolve(archivo);
    }
}
