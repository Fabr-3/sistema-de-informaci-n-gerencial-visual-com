package com.visual.backend.comercial.infraestructura.archivos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.visual.backend.comercial.infraestructura.configuracion.PropiedadesDocumentos;

class AlmacenDocumentosDiscoTest {

    private static final String NOMBRE = "CTR-2026-0001.pdf";

    @TempDir
    private Path directorio;

    private AlmacenDocumentosDisco almacen;

    @BeforeEach
    void preparar() {
        almacen = new AlmacenDocumentosDisco(new PropiedadesDocumentos(directorio.toString()));
    }

    @Test
    void guardaYReemplazaElDocumento() {
        almacen.guardar(NOMBRE, bytes("%PDF-primera"));
        almacen.guardar(NOMBRE, bytes("%PDF-segunda"));

        assertThat(almacen.leer(NOMBRE)).hasValueSatisfying(contenido ->
                assertThat(new String(contenido, StandardCharsets.US_ASCII)).isEqualTo("%PDF-segunda"));
        assertThat(directorio.toFile().list()).containsExactly(NOMBRE);
    }

    @Test
    void unDocumentoInexistenteNoTieneContenido() {
        assertThat(almacen.leer(NOMBRE)).isEmpty();
    }

    @Test
    void rechazaNombresQueSalenDeLaCarpeta() {
        assertThatThrownBy(() -> almacen.guardar("../fuera.pdf", bytes("%PDF-")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> almacen.leer("/etc/passwd")).isInstanceOf(IllegalArgumentException.class);
    }

    private static byte[] bytes(String texto) {
        return texto.getBytes(StandardCharsets.US_ASCII);
    }
}
