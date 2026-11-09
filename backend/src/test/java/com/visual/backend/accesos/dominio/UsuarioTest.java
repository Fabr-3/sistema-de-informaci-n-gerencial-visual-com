package com.visual.backend.accesos.dominio;

import static com.visual.backend.accesos.dominio.FabricaPruebasAccesos.AHORA;
import static com.visual.backend.accesos.dominio.FabricaPruebasAccesos.HASH;
import static com.visual.backend.accesos.dominio.FabricaPruebasAccesos.datos;
import static com.visual.backend.accesos.dominio.FabricaPruebasAccesos.rol;
import static com.visual.backend.accesos.dominio.FabricaPruebasAccesos.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.visual.backend.accesos.dominio.modelo.CodigoRol;
import com.visual.backend.accesos.dominio.modelo.PoliticaBloqueo;
import com.visual.backend.accesos.dominio.modelo.Usuario;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

class UsuarioTest {

    private static final PoliticaBloqueo POLITICA = new PoliticaBloqueo(3, Duration.ofMinutes(15));

    @Test
    void nuevoNormalizaDatosYExigeCambioDeContrasena() {
        Usuario usuario = Usuario.nuevo("  JPerez ", datos(), HASH, rol(CodigoRol.TECNICO), null, 1, AHORA);

        assertThat(usuario.getNombreUsuario()).isEqualTo("jperez");
        assertThat(usuario.getNombres()).isEqualTo("Juan");
        assertThat(usuario.getApellidoMaterno()).isNull();
        assertThat(usuario.getEmail()).isEqualTo("jperez@visual.com.bo");
        assertThat(usuario.isDebeCambiarContrasena()).isTrue();
        assertThat(usuario.permisosEfectivos()).isEmpty();
    }

    @Test
    void rechazaNombreDeUsuarioConFormatoInvalido() {
        assertThatThrownBy(() -> Usuario.normalizarNombreUsuario("ju an")).isInstanceOf(ReglaNegocioExcepcion.class);
        assertThatThrownBy(() -> Usuario.normalizarNombreUsuario("ab")).isInstanceOf(ReglaNegocioExcepcion.class);
    }

    @Test
    void subcontratadoRequiereFechaDeExpiracionFutura() {
        assertThatThrownBy(() -> Usuario.nuevo("temporal", datos(), HASH, rol(CodigoRol.SUBCONTRATADO_TEMPORAL),
                null, 1, AHORA)).hasMessageContaining("requiere una fecha de expiración");
        assertThatThrownBy(() -> Usuario.nuevo("temporal", datos(), HASH, rol(CodigoRol.SUBCONTRATADO_TEMPORAL),
                AHORA.minusDays(1), 1, AHORA)).hasMessageContaining("posterior a la fecha actual");
    }

    @Test
    void subcontratadoExpiradoSeRecontrataAmpliandoLaFecha() {
        Usuario usuario = usuario(5, CodigoRol.SUBCONTRATADO_TEMPORAL);
        assertThat(usuario.estaHabilitado(AHORA)).isTrue();
        assertThat(usuario.estaHabilitado(AHORA.plusDays(31))).isFalse();

        usuario.actualizar(datos(), usuario.getRol(), AHORA.plusDays(90), 1, AHORA.plusDays(31));

        assertThat(usuario.estaHabilitado(AHORA.plusDays(31))).isTrue();
    }

    @Test
    void permiteEditarDatosDeUnSubcontratadoExpiradoSinCambiarSuFecha() {
        Usuario usuario = usuario(5, CodigoRol.SUBCONTRATADO_TEMPORAL);

        usuario.actualizar(datos(), usuario.getRol(), usuario.getFechaExpiracion(), 1, AHORA.plusDays(60));

        assertThat(usuario.getEmail()).isEqualTo("jperez@visual.com.bo");
    }

    @Test
    void seBloqueaAlAlcanzarLosIntentosMaximos() {
        Usuario usuario = usuario(2, CodigoRol.TECNICO);

        assertThat(usuario.registrarIntentoFallido(POLITICA, AHORA)).isFalse();
        assertThat(usuario.registrarIntentoFallido(POLITICA, AHORA)).isFalse();
        assertThat(usuario.registrarIntentoFallido(POLITICA, AHORA)).isTrue();

        assertThat(usuario.estaBloqueado(AHORA.plusMinutes(14))).isTrue();
        assertThat(usuario.estaBloqueado(AHORA.plusMinutes(15))).isFalse();
        assertThat(usuario.getIntentosFallidos()).isZero();
    }

    @Test
    void accesoExitosoReiniciaIntentos() {
        Usuario usuario = usuario(2, CodigoRol.TECNICO);
        usuario.registrarIntentoFallido(POLITICA, AHORA);

        usuario.registrarAccesoExitoso(AHORA);

        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getFechaUltimoAcceso()).isEqualTo(AHORA);
    }

    @Test
    void restablecerContrasenaDesbloqueaYLaVuelveTemporal() {
        Usuario usuario = usuario(2, CodigoRol.TECNICO);
        for (int intento = 0; intento < POLITICA.intentosMaximos(); intento++) {
            usuario.registrarIntentoFallido(POLITICA, AHORA);
        }

        usuario.restablecerContrasena("nuevo-hash", 1, AHORA);

        assertThat(usuario.estaBloqueado(AHORA)).isFalse();
        assertThat(usuario.isDebeCambiarContrasena()).isTrue();
        assertThat(usuario.getHashContrasena()).isEqualTo("nuevo-hash");
    }

    @Test
    void obtienePermisosDelRolTrasCambiarLaContrasenaTemporal() {
        Usuario usuario = Usuario.nuevo("jperez", datos(), HASH, rol(CodigoRol.TECNICO), null, 1, AHORA);

        usuario.cambiarContrasena("otro-hash", AHORA);

        assertThat(usuario.permisosEfectivos()).containsExactly("ACTIVOS_VER");
    }

    @Test
    void eliminarEsBorradoLogico() {
        Usuario usuario = usuario(2, CodigoRol.TECNICO);

        usuario.eliminar(1, AHORA);

        assertThat(usuario.estaEliminado()).isTrue();
        assertThat(usuario.estaHabilitado(AHORA)).isFalse();
    }
}
