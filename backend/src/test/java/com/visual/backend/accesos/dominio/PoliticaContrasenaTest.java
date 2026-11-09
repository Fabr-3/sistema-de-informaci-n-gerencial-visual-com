package com.visual.backend.accesos.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.visual.backend.accesos.dominio.modelo.PoliticaContrasena;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

class PoliticaContrasenaTest {

    @ParameterizedTest
    @ValueSource(strings = {"admin123456", "Contrasena1", "ñandu2026"})
    void aceptaContrasenasConLetrasYNumeros(String contrasena) {
        assertThat(PoliticaContrasena.esValida(contrasena)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"corta1", "solamenteletras", "1234567890"})
    void rechazaContrasenasDebiles(String contrasena) {
        assertThatThrownBy(() -> PoliticaContrasena.validar(contrasena)).isInstanceOf(ReglaNegocioExcepcion.class);
    }

    @Test
    void rechazaContrasenasMasLargasQueElLimiteDeBcrypt() {
        String larga = "a1".repeat(PoliticaContrasena.LONGITUD_MAXIMA);

        assertThat(PoliticaContrasena.esValida(larga)).isFalse();
    }
}
