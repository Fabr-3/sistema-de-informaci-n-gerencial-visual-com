package com.visual.backend.accesos.dominio.modelo;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Usuario del sistema. Concentra las reglas de habilitacion de la cuenta:
 * estado activo, vigencia (sub-contratados), bloqueo por intentos fallidos
 * y borrado logico. El estado solo cambia mediante sus metodos de negocio;
 * el builder se reserva para reconstruir usuarios desde la persistencia (las
 * cuentas nuevas se crean con {@link #nuevo}).
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Usuario {

    private static final Pattern FORMATO_NOMBRE_USUARIO = Pattern.compile("^[a-z0-9._-]{3,50}$");

    private Integer id;
    private String nombreUsuario;
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String email;
    private String hashContrasena;
    private Rol rol;
    private boolean activo;
    private LocalDateTime fechaExpiracion;
    private int intentosFallidos;
    private LocalDateTime bloqueadoHasta;
    private boolean debeCambiarContrasena;
    private LocalDateTime fechaUltimoAcceso;
    private Integer creadoPor;
    private Integer modificadoPor;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
    private LocalDateTime fechaEliminacion;

    /**
     * Crea una cuenta nueva. La contrasena es temporal: el usuario debe
     * cambiarla en su primer inicio de sesion.
     */
    public static Usuario nuevo(String nombreUsuario, DatosPersonales datos, String hashContrasena, Rol rol,
                                LocalDateTime fechaExpiracion, Integer creadoPor, LocalDateTime ahora) {
        validarVigencia(rol, fechaExpiracion, ahora);
        return Usuario.builder()
                .nombreUsuario(normalizarNombreUsuario(nombreUsuario))
                .nombres(datos.nombres())
                .apellidoPaterno(datos.apellidoPaterno())
                .apellidoMaterno(datos.apellidoMaterno())
                .email(datos.email())
                .hashContrasena(hashContrasena)
                .rol(rol)
                .activo(true)
                .fechaExpiracion(fechaExpiracion)
                .debeCambiarContrasena(true)
                .creadoPor(creadoPor)
                .fechaCreacion(ahora)
                .build();
    }

    /** Normaliza y valida el nombre de usuario (minusculas, 3 a 50 caracteres). */
    public static String normalizarNombreUsuario(String nombreUsuario) {
        String normalizado = nombreUsuario == null ? "" : nombreUsuario.trim().toLowerCase(Locale.ROOT);
        if (!FORMATO_NOMBRE_USUARIO.matcher(normalizado).matches()) {
            throw new ReglaNegocioExcepcion(
                    "El nombre de usuario debe tener de 3 a 50 caracteres: letras minúsculas, números, punto, guion o guion bajo");
        }
        return normalizado;
    }

    public boolean estaEliminado() {
        return fechaEliminacion != null;
    }

    public boolean estaBloqueado(LocalDateTime ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    public boolean estaExpirado(LocalDateTime ahora) {
        return fechaExpiracion != null && !fechaExpiracion.isAfter(ahora);
    }

    /** Una cuenta habilitada puede iniciar sesion (si sus credenciales son correctas). */
    public boolean estaHabilitado(LocalDateTime ahora) {
        return activo && !estaEliminado() && !estaExpirado(ahora);
    }

    /**
     * Suma un intento fallido; al alcanzar el maximo bloquea la cuenta y
     * reinicia el contador.
     *
     * @return true si este intento provoco el bloqueo
     */
    public boolean registrarIntentoFallido(PoliticaBloqueo politica, LocalDateTime ahora) {
        intentosFallidos++;
        if (intentosFallidos < politica.intentosMaximos()) {
            return false;
        }
        bloqueadoHasta = ahora.plus(politica.duracionBloqueo());
        intentosFallidos = 0;
        return true;
    }

    public void registrarAccesoExitoso(LocalDateTime ahora) {
        intentosFallidos = 0;
        bloqueadoHasta = null;
        fechaUltimoAcceso = ahora;
    }

    /** Cambio realizado por el propio usuario: deja de requerir cambio obligatorio. */
    public void cambiarContrasena(String nuevoHash, LocalDateTime ahora) {
        hashContrasena = nuevoHash;
        debeCambiarContrasena = false;
        registrarModificacion(id, ahora);
    }

    /**
     * Marca la contrasena asignada al crear la cuenta como definitiva (solo
     * para el administrador inicial cuando la configuracion lo indica).
     */
    public void confirmarContrasenaInicial() {
        debeCambiarContrasena = false;
    }

    /**
     * Restablecimiento por el administrador: la nueva contrasena es temporal
     * y se levanta cualquier bloqueo vigente.
     */
    public void restablecerContrasena(String nuevoHash, Integer actor, LocalDateTime ahora) {
        hashContrasena = nuevoHash;
        debeCambiarContrasena = true;
        intentosFallidos = 0;
        bloqueadoHasta = null;
        registrarModificacion(actor, ahora);
    }

    public void actualizar(DatosPersonales datos, Rol nuevoRol, LocalDateTime nuevaFechaExpiracion,
                           Integer actor, LocalDateTime ahora) {
        // Solo se exige fecha futura si cambia: permite editar otros datos de
        // un sub-contratado cuya vigencia ya termino sin recontratarlo.
        boolean cambiaVigencia = nuevaFechaExpiracion != null && !nuevaFechaExpiracion.equals(fechaExpiracion);
        validarVigencia(nuevoRol, nuevaFechaExpiracion, cambiaVigencia ? ahora : null);
        nombres = datos.nombres();
        apellidoPaterno = datos.apellidoPaterno();
        apellidoMaterno = datos.apellidoMaterno();
        email = datos.email();
        rol = nuevoRol;
        fechaExpiracion = nuevaFechaExpiracion;
        registrarModificacion(actor, ahora);
    }

    public void cambiarEstado(boolean nuevoEstado, Integer actor, LocalDateTime ahora) {
        activo = nuevoEstado;
        registrarModificacion(actor, ahora);
    }

    /** Borrado logico: el registro se conserva por la auditoria y las referencias historicas. */
    public void eliminar(Integer actor, LocalDateTime ahora) {
        activo = false;
        fechaEliminacion = ahora;
        registrarModificacion(actor, ahora);
    }

    public boolean esAdministradorHabilitado(LocalDateTime ahora) {
        return rol.esAdministrador() && estaHabilitado(ahora);
    }

    /**
     * Permisos que se otorgan en la sesion. Mientras el usuario deba cambiar
     * su contrasena temporal no recibe ninguno: solo puede cambiarla.
     */
    public Set<String> permisosEfectivos() {
        if (debeCambiarContrasena) {
            return Set.of();
        }
        return rol.getPermisos().stream().map(Permiso::nombre).collect(Collectors.toUnmodifiableSet());
    }

    public String nombreCompleto() {
        StringBuilder completo = new StringBuilder(nombres).append(' ').append(apellidoPaterno);
        if (apellidoMaterno != null) {
            completo.append(' ').append(apellidoMaterno);
        }
        return completo.toString();
    }

    private void registrarModificacion(Integer actor, LocalDateTime ahora) {
        modificadoPor = actor;
        fechaModificacion = ahora;
    }

    /**
     * Los sub-contratados temporales requieren fecha de expiracion. Si se
     * recibe "ahora", ademas la fecha debe ser futura.
     */
    private static void validarVigencia(Rol rol, LocalDateTime fechaExpiracion, LocalDateTime ahora) {
        if (rol.getCodigo().requiereFechaExpiracion() && fechaExpiracion == null) {
            throw new ReglaNegocioExcepcion("El rol " + rol.getNombre() + " requiere una fecha de expiración");
        }
        if (ahora != null && fechaExpiracion != null && !fechaExpiracion.isAfter(ahora)) {
            throw new ReglaNegocioExcepcion("La fecha de expiración debe ser posterior a la fecha actual");
        }
    }
}
