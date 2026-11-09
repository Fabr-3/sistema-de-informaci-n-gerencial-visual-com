package com.visual.backend.accesos.infraestructura.seguridad;

import java.time.Clock;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.dto.VistaUsuario;
import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.aplicacion.servicio.ServicioUsuarios;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.modelo.CodigoRol;
import com.visual.backend.accesos.dominio.modelo.DatosPersonales;
import com.visual.backend.accesos.dominio.modelo.PoliticaContrasena;
import com.visual.backend.accesos.dominio.modelo.Rol;
import com.visual.backend.accesos.dominio.modelo.Usuario;
import com.visual.backend.accesos.dominio.puerto.CodificadorContrasenas;
import com.visual.backend.accesos.dominio.puerto.RepositorioRoles;
import com.visual.backend.accesos.dominio.puerto.RepositorioUsuarios;

/**
 * Crea el administrador inicial si el sistema no tiene ningun administrador.
 * Las credenciales provienen de la configuracion (variables de entorno fuera
 * del perfil de desarrollo), nunca de las migraciones.
 */
@Component
public class InicializadorAdministrador implements ApplicationRunner {

    private static final Logger REGISTRO = LoggerFactory.getLogger(InicializadorAdministrador.class);

    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioRoles repositorioRoles;
    private final CodificadorContrasenas codificador;
    private final ServicioAuditoria auditoria;
    private final PropiedadesSeguridad.AdministradorInicial configuracion;
    private final Clock reloj;

    public InicializadorAdministrador(RepositorioUsuarios repositorioUsuarios, RepositorioRoles repositorioRoles,
                                      CodificadorContrasenas codificador, ServicioAuditoria auditoria,
                                      PropiedadesSeguridad propiedades, Clock reloj) {
        this.repositorioUsuarios = repositorioUsuarios;
        this.repositorioRoles = repositorioRoles;
        this.codificador = codificador;
        this.auditoria = auditoria;
        this.configuracion = propiedades.administradorInicial();
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (repositorioUsuarios.existeAdministrador()) {
            return;
        }
        PoliticaContrasena.validar(configuracion.contrasena());
        Rol rolAdministrador = repositorioRoles.buscarPorCodigo(CodigoRol.ADMINISTRADOR)
                .orElseThrow(() -> new IllegalStateException("No existe el rol ADMINISTRADOR; revise las migraciones"));
        LocalDateTime ahora = LocalDateTime.now(reloj);

        Usuario administrador = Usuario.nuevo(configuracion.nombreUsuario(),
                new DatosPersonales(configuracion.nombres(), configuracion.apellidoPaterno(), null,
                        configuracion.email()),
                codificador.codificar(configuracion.contrasena()), rolAdministrador, null, null, ahora);
        if (!configuracion.debeCambiarContrasena()) {
            administrador.confirmarContrasenaInicial();
        }

        VistaUsuario creado = VistaUsuario.de(repositorioUsuarios.guardar(administrador), ahora);
        auditoria.registrarOperacion(AccionAuditoria.CREAR, ServicioUsuarios.ENTIDAD, creado.id(), null,
                creado.comoMapaAuditoria(), "Administrador inicial");
        REGISTRO.info("Administrador inicial '{}' creado", creado.nombreUsuario());
    }
}
