package com.visual.backend.accesos.dominio;

import java.time.LocalDateTime;
import java.util.Set;

import com.visual.backend.accesos.dominio.modelo.CodigoRol;
import com.visual.backend.accesos.dominio.modelo.DatosPersonales;
import com.visual.backend.accesos.dominio.modelo.Permiso;
import com.visual.backend.accesos.dominio.modelo.Rol;
import com.visual.backend.accesos.dominio.modelo.Usuario;

/** Datos de prueba reutilizables del modulo de accesos. */
public final class FabricaPruebasAccesos {

    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 8, 10, 0);
    public static final String HASH = "hash-prueba";
    public static final Permiso PERMISO_VER_ACTIVOS = new Permiso(7, "ACTIVOS", "ACTIVOS_VER", "Consultar activos");

    private FabricaPruebasAccesos() {
    }

    public static Rol rol(CodigoRol codigo) {
        return new Rol(codigo.ordinal() + 1, codigo, codigo.name(), null, Set.of(PERMISO_VER_ACTIVOS));
    }

    public static DatosPersonales datos() {
        return new DatosPersonales(" Juan ", "Perez", "  ", "JPerez@Visual.com.bo");
    }

    /** Usuario persistido (con id), habilitado y con contrasena definitiva. */
    public static Usuario usuario(Integer id, CodigoRol codigo) {
        LocalDateTime expiracion = codigo.requiereFechaExpiracion() ? AHORA.plusDays(30) : null;
        return Usuario.builder()
                .id(id)
                .nombreUsuario("usuario" + id)
                .nombres("Juan")
                .apellidoPaterno("Perez")
                .email("usuario" + id + "@visual.com.bo")
                .hashContrasena(HASH)
                .rol(rol(codigo))
                .activo(true)
                .fechaExpiracion(expiracion)
                .fechaCreacion(AHORA)
                .build();
    }
}
