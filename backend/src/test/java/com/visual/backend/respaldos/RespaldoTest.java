package com.visual.backend.respaldos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.visual.backend.respaldos.dominio.Respaldo;

class RespaldoTest {

    @Test
    void elNombreGeneradoEsValido() {
        String nombre = Respaldo.nombreParaFecha(LocalDateTime.of(2026, 11, 8, 2, 0, 5, 123_000_000));

        assertThat(nombre).isEqualTo("respaldo_20261108_020005_123.dump");
        assertThat(Respaldo.esNombreValido(nombre)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"../pom.xml", "respaldo_20261108_020005_123.dump/../../x", "otro.dump",
            "respaldo_20261108_020005_123.sql"})
    void rechazaNombresQuePodrianSalirDelDirectorio(String nombre) {
        assertThat(Respaldo.esNombreValido(nombre)).isFalse();
    }
}
