package com.visual.backend.accesos.aplicacion;

import static com.visual.backend.accesos.dominio.FabricaPruebasAccesos.AHORA;
import static com.visual.backend.accesos.dominio.FabricaPruebasAccesos.HASH;
import static com.visual.backend.accesos.dominio.FabricaPruebasAccesos.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.visual.backend.accesos.aplicacion.dto.ComandoCambioContrasena;
import com.visual.backend.accesos.aplicacion.dto.ComandoInicioSesion;
import com.visual.backend.accesos.aplicacion.dto.ResultadoSesion;
import com.visual.backend.accesos.aplicacion.servicio.ParametrosSesion;
import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.aplicacion.servicio.ServicioAutenticacion;
import com.visual.backend.accesos.dominio.excepcion.AutenticacionExcepcion;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.modelo.CodigoRol;
import com.visual.backend.accesos.dominio.modelo.PoliticaBloqueo;
import com.visual.backend.accesos.dominio.modelo.TokenRefresco;
import com.visual.backend.accesos.dominio.modelo.Usuario;
import com.visual.backend.accesos.dominio.puerto.CodificadorContrasenas;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.accesos.dominio.puerto.EmisorTokensAcceso;
import com.visual.backend.accesos.dominio.puerto.EmisorTokensAcceso.TokenAcceso;
import com.visual.backend.accesos.dominio.puerto.GeneradorTokensRefresco;
import com.visual.backend.accesos.dominio.puerto.RepositorioTokensRefresco;
import com.visual.backend.accesos.dominio.puerto.RepositorioUsuarios;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServicioAutenticacionTest {

    private static final String CONTRASENA = "correcta123";
    private static final String TOKEN_PLANO = "token-plano";
    private static final String HASH_TOKEN = "hash-token";

    @Mock
    private RepositorioUsuarios repositorioUsuarios;
    @Mock
    private RepositorioTokensRefresco repositorioTokens;
    @Mock
    private CodificadorContrasenas codificador;
    @Mock
    private EmisorTokensAcceso emisorTokens;
    @Mock
    private GeneradorTokensRefresco generadorTokens;
    @Mock
    private ServicioAuditoria auditoria;
    @Mock
    private ContextoSolicitud contexto;

    private ServicioAutenticacion servicio;

    @BeforeEach
    void configurar() {
        Clock reloj = Clock.fixed(AHORA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        when(codificador.codificar(anyString())).thenReturn("hash-simulado");
        when(codificador.coincide(CONTRASENA, HASH)).thenReturn(true);
        when(generadorTokens.generar()).thenReturn(TOKEN_PLANO);
        when(generadorTokens.calcularHash(TOKEN_PLANO)).thenReturn(HASH_TOKEN);
        when(emisorTokens.emitir(any())).thenReturn(new TokenAcceso("jwt", Instant.EPOCH));
        when(repositorioUsuarios.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        servicio = new ServicioAutenticacion(repositorioUsuarios, repositorioTokens, codificador, emisorTokens,
                generadorTokens, auditoria, contexto, new PoliticaBloqueo(2, Duration.ofMinutes(15)),
                new ParametrosSesion(Duration.ofDays(7)), reloj);
    }

    @Test
    void inicioExitosoAbreSesionYAudita() {
        Usuario usuario = usuario(2, CodigoRol.TECNICO);
        when(repositorioUsuarios.buscarPorNombreUsuario("usuario2")).thenReturn(Optional.of(usuario));

        ResultadoSesion resultado = servicio.iniciarSesion(new ComandoInicioSesion(" USUARIO2 ", CONTRASENA));

        assertThat(resultado.tokenRefresco()).isEqualTo(TOKEN_PLANO);
        assertThat(resultado.usuario().permisos()).containsExactly("ACTIVOS_VER");
        assertThat(usuario.getFechaUltimoAcceso()).isEqualTo(AHORA);
        verify(repositorioTokens).guardar(any(TokenRefresco.class));
        verify(auditoria).registrarAcceso(AccionAuditoria.INICIO_SESION, 2, "usuario2", null);
    }

    @Test
    void usuarioInexistenteRespondeConMensajeGenericoYCompararContrasena() {
        when(repositorioUsuarios.buscarPorNombreUsuario("fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.iniciarSesion(new ComandoInicioSesion("fantasma", CONTRASENA)))
                .isInstanceOf(AutenticacionExcepcion.class)
                .hasMessage(AutenticacionExcepcion.CREDENCIALES_INVALIDAS);
        verify(codificador).coincide(CONTRASENA, "hash-simulado");
        verify(auditoria).registrarAcceso(eq(AccionAuditoria.INICIO_SESION_FALLIDO), isNull(), eq("fantasma"),
                anyString());
    }

    @Test
    void contrasenaIncorrectaRepetidaBloqueaLaCuenta() {
        Usuario usuario = usuario(2, CodigoRol.TECNICO);
        when(repositorioUsuarios.buscarPorNombreUsuario("usuario2")).thenReturn(Optional.of(usuario));
        ComandoInicioSesion comandoErroneo = new ComandoInicioSesion("usuario2", "incorrecta1");

        assertThatThrownBy(() -> servicio.iniciarSesion(comandoErroneo)).isInstanceOf(AutenticacionExcepcion.class);
        assertThatThrownBy(() -> servicio.iniciarSesion(comandoErroneo)).isInstanceOf(AutenticacionExcepcion.class);

        assertThat(usuario.estaBloqueado(AHORA)).isTrue();
        verify(auditoria).registrarAcceso(eq(AccionAuditoria.BLOQUEO_CUENTA), eq(2), eq("usuario2"), anyString());
        assertThatThrownBy(() -> servicio.iniciarSesion(new ComandoInicioSesion("usuario2", CONTRASENA)))
                .hasMessage(AutenticacionExcepcion.CUENTA_BLOQUEADA);
    }

    @Test
    void cuentaInactivaNoPuedeIniciarSesion() {
        Usuario usuario = usuario(2, CodigoRol.TECNICO);
        usuario.cambiarEstado(false, 1, AHORA);
        when(repositorioUsuarios.buscarPorNombreUsuario("usuario2")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> servicio.iniciarSesion(new ComandoInicioSesion("usuario2", CONTRASENA)))
                .hasMessage(AutenticacionExcepcion.CUENTA_NO_HABILITADA);
        verify(repositorioTokens, never()).guardar(any());
    }

    @Test
    void refrescoRotaElToken() {
        TokenRefresco token = TokenRefresco.nuevo(2, HASH_TOKEN, AHORA.minusHours(1), AHORA.plusDays(1));
        when(repositorioTokens.buscarPorHash(HASH_TOKEN)).thenReturn(Optional.of(token));
        when(repositorioUsuarios.buscarPorId(2)).thenReturn(Optional.of(usuario(2, CodigoRol.TECNICO)));

        servicio.refrescarSesion(TOKEN_PLANO);

        assertThat(token.estaRevocado()).isTrue();
    }

    @Test
    void reutilizarUnTokenRevocadoCierraTodasLasSesiones() {
        TokenRefresco token = new TokenRefresco(9L, 2, HASH_TOKEN, AHORA.minusHours(2), AHORA.plusDays(1),
                AHORA.minusHours(1));
        when(repositorioTokens.buscarPorHash(HASH_TOKEN)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> servicio.refrescarSesion(TOKEN_PLANO))
                .hasMessage(AutenticacionExcepcion.SESION_INVALIDA);
        verify(repositorioTokens).revocarTodosDelUsuario(2, AHORA);
    }

    @Test
    void cambioDeContrasenaExigeLaActualCorrecta() {
        Usuario usuario = usuario(2, CodigoRol.TECNICO);
        when(contexto.usuarioActual()).thenReturn(Optional.of(new ContextoSolicitud.UsuarioActual(2, "usuario2")));
        when(repositorioUsuarios.buscarPorId(2)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> servicio.cambiarContrasenaPropia(
                new ComandoCambioContrasena("incorrecta1", "nueva12345")))
                .isInstanceOf(ReglaNegocioExcepcion.class);

        servicio.cambiarContrasenaPropia(new ComandoCambioContrasena(CONTRASENA, "nueva12345"));

        assertThat(usuario.isDebeCambiarContrasena()).isFalse();
        verify(repositorioTokens).revocarTodosDelUsuario(2, AHORA);
        verify(auditoria).registrarAcceso(AccionAuditoria.CAMBIO_CONTRASENA, 2, "usuario2", null);
    }
}
