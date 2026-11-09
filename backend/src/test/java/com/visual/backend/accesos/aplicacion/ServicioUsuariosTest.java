package com.visual.backend.accesos.aplicacion;

import static com.visual.backend.accesos.dominio.FabricaPruebasAccesos.AHORA;
import static com.visual.backend.accesos.dominio.FabricaPruebasAccesos.rol;
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
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.visual.backend.accesos.aplicacion.dto.ComandoActualizarUsuario;
import com.visual.backend.accesos.aplicacion.dto.ComandoCrearUsuario;
import com.visual.backend.accesos.aplicacion.dto.VistaUsuario;
import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.aplicacion.servicio.ServicioUsuarios;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.modelo.CodigoRol;
import com.visual.backend.accesos.dominio.modelo.Usuario;
import com.visual.backend.accesos.dominio.puerto.CodificadorContrasenas;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.accesos.dominio.puerto.RepositorioRoles;
import com.visual.backend.accesos.dominio.puerto.RepositorioTokensRefresco;
import com.visual.backend.accesos.dominio.puerto.RepositorioUsuarios;
import com.visual.backend.compartido.dominio.excepcion.DatoDuplicadoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServicioUsuariosTest {

    private static final int ID_ADMINISTRADOR_ACTUAL = 1;

    @Mock
    private RepositorioUsuarios repositorioUsuarios;
    @Mock
    private RepositorioRoles repositorioRoles;
    @Mock
    private RepositorioTokensRefresco repositorioTokens;
    @Mock
    private CodificadorContrasenas codificador;
    @Mock
    private ServicioAuditoria auditoria;
    @Mock
    private ContextoSolicitud contexto;

    private ServicioUsuarios servicio;

    @BeforeEach
    void configurar() {
        Clock reloj = Clock.fixed(AHORA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        when(contexto.usuarioActual()).thenReturn(Optional.of(
                new ContextoSolicitud.UsuarioActual(ID_ADMINISTRADOR_ACTUAL, "admin")));
        when(codificador.codificar(anyString())).thenReturn("hash-nuevo");
        for (CodigoRol codigo : CodigoRol.values()) {
            when(repositorioRoles.buscarPorId(codigo.ordinal() + 1)).thenReturn(Optional.of(rol(codigo)));
        }
        when(repositorioUsuarios.guardar(any())).thenAnswer(invocacion -> {
            Usuario usuario = invocacion.getArgument(0);
            return usuario.getId() == null ? conId(usuario, 10) : usuario;
        });
        servicio = new ServicioUsuarios(repositorioUsuarios, repositorioRoles, repositorioTokens, codificador,
                auditoria, contexto, reloj);
    }

    @Test
    void creaUsuarioConContrasenaTemporalYAudita() {
        VistaUsuario creado = servicio.crear(comandoCrear("ana.lopez", "ana@visual.com.bo"));

        assertThat(creado.nombreUsuario()).isEqualTo("ana.lopez");
        assertThat(creado.debeCambiarContrasena()).isTrue();
        verify(auditoria).registrarOperacion(eq(AccionAuditoria.CREAR), eq(ServicioUsuarios.ENTIDAD), eq(10),
                isNull(), any(), isNull());
    }

    @Test
    void rechazaNombreDeUsuarioDuplicado() {
        when(repositorioUsuarios.existeNombreUsuario("ana.lopez")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(comandoCrear("Ana.Lopez", "ana@visual.com.bo")))
                .isInstanceOf(DatoDuplicadoExcepcion.class);
        verify(repositorioUsuarios, never()).guardar(any());
    }

    @Test
    void rechazaContrasenaTemporalDebil() {
        ComandoCrearUsuario comando = new ComandoCrearUsuario("ana.lopez", "Ana", "Lopez", null,
                "ana@visual.com.bo", CodigoRol.TECNICO.ordinal() + 1, null, "debil");

        assertThatThrownBy(() -> servicio.crear(comando)).isInstanceOf(ReglaNegocioExcepcion.class);
    }

    @Test
    void elAdministradorNoPuedeDesactivarseNiEliminarseASiMismo() {
        when(repositorioUsuarios.buscarPorId(ID_ADMINISTRADOR_ACTUAL))
                .thenReturn(Optional.of(usuario(ID_ADMINISTRADOR_ACTUAL, CodigoRol.ADMINISTRADOR)));

        assertThatThrownBy(() -> servicio.cambiarEstado(ID_ADMINISTRADOR_ACTUAL, false))
                .hasMessageContaining("propia cuenta");
        assertThatThrownBy(() -> servicio.eliminar(ID_ADMINISTRADOR_ACTUAL))
                .hasMessageContaining("propia cuenta");
    }

    @Test
    void noPermiteDejarAlSistemaSinAdministradores() {
        when(repositorioUsuarios.buscarPorId(2)).thenReturn(Optional.of(usuario(2, CodigoRol.ADMINISTRADOR)));
        when(repositorioUsuarios.contarOtrosAdministradoresHabilitados(2, AHORA)).thenReturn(0L);

        assertThatThrownBy(() -> servicio.eliminar(2)).hasMessageContaining("al menos un administrador");
    }

    @Test
    void cambiarElRolCierraLasSesionesDelUsuario() {
        when(repositorioUsuarios.buscarPorId(2)).thenReturn(Optional.of(usuario(2, CodigoRol.TECNICO)));
        ComandoActualizarUsuario comando = new ComandoActualizarUsuario("Juan", "Perez", null,
                "usuario2@visual.com.bo", CodigoRol.GERENTE.ordinal() + 1, null);

        VistaUsuario actualizado = servicio.actualizar(2, comando);

        assertThat(actualizado.codigoRol()).isEqualTo(CodigoRol.GERENTE.name());
        verify(repositorioTokens).revocarTodosDelUsuario(2, AHORA);
    }

    @Test
    void desactivarCierraLasSesionesDelUsuario() {
        when(repositorioUsuarios.buscarPorId(2)).thenReturn(Optional.of(usuario(2, CodigoRol.TECNICO)));

        VistaUsuario actualizado = servicio.cambiarEstado(2, false);

        assertThat(actualizado.activo()).isFalse();
        verify(repositorioTokens).revocarTodosDelUsuario(2, AHORA);
    }

    private static ComandoCrearUsuario comandoCrear(String nombreUsuario, String email) {
        return new ComandoCrearUsuario(nombreUsuario, "Ana", "Lopez", null, email,
                CodigoRol.TECNICO.ordinal() + 1, null, "temporal123");
    }

    private static Usuario conId(Usuario usuario, Integer id) {
        return Usuario.builder()
                .id(id)
                .nombreUsuario(usuario.getNombreUsuario())
                .nombres(usuario.getNombres())
                .apellidoPaterno(usuario.getApellidoPaterno())
                .email(usuario.getEmail())
                .hashContrasena(usuario.getHashContrasena())
                .rol(usuario.getRol())
                .activo(usuario.isActivo())
                .debeCambiarContrasena(usuario.isDebeCambiarContrasena())
                .fechaCreacion(usuario.getFechaCreacion())
                .build();
    }
}
