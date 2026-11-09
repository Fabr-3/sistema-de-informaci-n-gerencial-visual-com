package com.visual.backend.respaldos.infraestructura;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.visual.backend.respaldos.dominio.AlmacenRespaldos;
import com.visual.backend.respaldos.dominio.Respaldo;
import com.visual.backend.respaldos.dominio.RespaldoExcepcion;

/**
 * Genera respaldos con pg_dump en formato personalizado (-Fc), restaurable con
 * pg_restore. El proceso se invoca con una lista de argumentos (sin shell),
 * por lo que no hay riesgo de inyeccion de comandos, y la contrasena se pasa
 * por la variable de entorno PGPASSWORD, no por la linea de comandos.
 */
@Component
public class AlmacenRespaldosPgDump implements AlmacenRespaldos {

    private static final Logger REGISTRO = LoggerFactory.getLogger(AlmacenRespaldosPgDump.class);
    private static final String PREFIJO_JDBC = "jdbc:";
    private static final int PUERTO_POR_DEFECTO = 5432;
    private static final int CODIGO_EXITO = 0;

    private final PropiedadesRespaldos propiedades;
    private final Clock reloj;
    private final URI urlBaseDatos;
    private final String usuarioBaseDatos;
    private final String contrasenaBaseDatos;

    public AlmacenRespaldosPgDump(PropiedadesRespaldos propiedades, Clock reloj,
                                  @Value("${spring.datasource.url}") String urlJdbc,
                                  @Value("${spring.datasource.username}") String usuarioBaseDatos,
                                  @Value("${spring.datasource.password}") String contrasenaBaseDatos) {
        this.propiedades = propiedades;
        this.reloj = reloj;
        this.urlBaseDatos = URI.create(urlJdbc.substring(PREFIJO_JDBC.length()));
        this.usuarioBaseDatos = usuarioBaseDatos;
        this.contrasenaBaseDatos = contrasenaBaseDatos;
    }

    /** synchronized: evita dos pg_dump simultaneos sobre la misma base de datos. */
    @Override
    public synchronized Respaldo generar() {
        Path directorio = directorio();
        Path destino = directorio.resolve(Respaldo.nombreParaFecha(LocalDateTime.now(reloj)));
        Path registroErrores = null;
        try {
            Files.createDirectories(directorio);
            registroErrores = Files.createTempFile(directorio, "pg_dump_", ".log");
            ejecutarPgDump(destino, registroErrores);
            return aRespaldo(destino);
        } catch (IOException e) {
            eliminarSilenciosamente(destino);
            throw new RespaldoExcepcion("No se pudo generar el respaldo", e);
        } finally {
            eliminarSilenciosamente(registroErrores);
        }
    }

    @Override
    public List<Respaldo> listar() {
        Path directorio = directorio();
        if (!Files.isDirectory(directorio)) {
            return List.of();
        }
        try (Stream<Path> archivos = Files.list(directorio)) {
            return archivos
                    .filter(archivo -> Respaldo.esNombreValido(archivo.getFileName().toString()))
                    .map(this::aRespaldo)
                    .sorted(Comparator.comparing(Respaldo::fechaCreacion).reversed())
                    .toList();
        } catch (IOException e) {
            throw new RespaldoExcepcion("No se pudo listar los respaldos", e);
        }
    }

    @Override
    public Optional<Path> ubicar(String nombreArchivo) {
        if (!Respaldo.esNombreValido(nombreArchivo)) {
            return Optional.empty();
        }
        Path ruta = directorio().resolve(nombreArchivo).normalize();
        return ruta.startsWith(directorio()) && Files.isRegularFile(ruta) ? Optional.of(ruta) : Optional.empty();
    }

    private void ejecutarPgDump(Path destino, Path registroErrores) throws IOException {
        ProcessBuilder constructor = new ProcessBuilder(
                propiedades.rutaPgDump(),
                "--host=" + urlBaseDatos.getHost(),
                "--port=" + (urlBaseDatos.getPort() > 0 ? urlBaseDatos.getPort() : PUERTO_POR_DEFECTO),
                "--username=" + usuarioBaseDatos,
                "--dbname=" + nombreBaseDatos(),
                "--format=custom",
                "--no-password",
                "--file=" + destino);
        constructor.environment().put("PGPASSWORD", contrasenaBaseDatos);
        constructor.redirectErrorStream(true);
        constructor.redirectOutput(registroErrores.toFile());

        Process proceso = constructor.start();
        try {
            if (!proceso.waitFor(propiedades.tiempoMaximo().toMillis(), TimeUnit.MILLISECONDS)) {
                proceso.destroyForcibly();
                throw new RespaldoExcepcion("El respaldo excedió el tiempo máximo de " + propiedades.tiempoMaximo());
            }
        } catch (InterruptedException e) {
            proceso.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new RespaldoExcepcion("El respaldo fue interrumpido", e);
        }
        if (proceso.exitValue() != CODIGO_EXITO) {
            REGISTRO.error("pg_dump finalizo con codigo {}: {}", proceso.exitValue(), Files.readString(registroErrores));
            eliminarSilenciosamente(destino);
            throw new RespaldoExcepcion("pg_dump finalizó con errores (código " + proceso.exitValue() + ")");
        }
    }

    /** Ruta de la URL sin la barra inicial ni parametros: jdbc:postgresql://host:5432/base?x=y. */
    private String nombreBaseDatos() {
        return urlBaseDatos.getPath().substring(1);
    }

    private Path directorio() {
        return Path.of(propiedades.directorio()).toAbsolutePath().normalize();
    }

    private Respaldo aRespaldo(Path archivo) {
        try {
            LocalDateTime fecha = LocalDateTime.ofInstant(Files.getLastModifiedTime(archivo).toInstant(),
                    reloj.getZone());
            return new Respaldo(archivo.getFileName().toString(), Files.size(archivo), fecha);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void eliminarSilenciosamente(Path archivo) {
        if (archivo == null) {
            return;
        }
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            REGISTRO.warn("No se pudo eliminar el archivo temporal {}", archivo, e);
        }
    }
}
