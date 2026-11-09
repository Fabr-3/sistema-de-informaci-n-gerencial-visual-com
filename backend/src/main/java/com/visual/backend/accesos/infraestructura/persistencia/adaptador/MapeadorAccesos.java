package com.visual.backend.accesos.infraestructura.persistencia.adaptador;

import java.util.stream.Collectors;

import com.visual.backend.accesos.dominio.modelo.Permiso;
import com.visual.backend.accesos.dominio.modelo.Rol;
import com.visual.backend.accesos.dominio.modelo.TokenRefresco;
import com.visual.backend.accesos.dominio.modelo.Usuario;
import com.visual.backend.accesos.infraestructura.persistencia.entidad.PermisoEntidad;
import com.visual.backend.accesos.infraestructura.persistencia.entidad.RolEntidad;
import com.visual.backend.accesos.infraestructura.persistencia.entidad.TokenRefrescoEntidad;
import com.visual.backend.accesos.infraestructura.persistencia.entidad.UsuarioEntidad;

/** Conversion entre entidades JPA y modelos de dominio del modulo de accesos. */
final class MapeadorAccesos {

    private MapeadorAccesos() {
    }

    static Permiso aDominio(PermisoEntidad entidad) {
        return new Permiso(entidad.getId(), entidad.getModulo(), entidad.getNombre(), entidad.getDescripcion());
    }

    static Rol aDominio(RolEntidad entidad) {
        return new Rol(entidad.getId(), entidad.getCodigo(), entidad.getNombre(), entidad.getDescripcion(),
                entidad.getPermisos().stream().map(MapeadorAccesos::aDominio).collect(Collectors.toSet()));
    }

    static Usuario aDominio(UsuarioEntidad entidad) {
        return Usuario.builder()
                .id(entidad.getId())
                .nombreUsuario(entidad.getNombreUsuario())
                .nombres(entidad.getNombres())
                .apellidoPaterno(entidad.getApellidoPaterno())
                .apellidoMaterno(entidad.getApellidoMaterno())
                .email(entidad.getEmail())
                .hashContrasena(entidad.getHashContrasena())
                .rol(aDominio(entidad.getRol()))
                .activo(entidad.isActivo())
                .fechaExpiracion(entidad.getFechaExpiracion())
                .intentosFallidos(entidad.getIntentosFallidos())
                .bloqueadoHasta(entidad.getBloqueadoHasta())
                .debeCambiarContrasena(entidad.isDebeCambiarContrasena())
                .fechaUltimoAcceso(entidad.getFechaUltimoAcceso())
                .creadoPor(entidad.getCreadoPor())
                .modificadoPor(entidad.getModificadoPor())
                .fechaCreacion(entidad.getFechaCreacion())
                .fechaModificacion(entidad.getFechaModificacion())
                .fechaEliminacion(entidad.getFechaEliminacion())
                .build();
    }

    /** Copia el estado del dominio sobre la entidad (nueva o administrada). */
    static void copiar(Usuario origen, UsuarioEntidad destino, RolEntidad rol) {
        destino.setNombreUsuario(origen.getNombreUsuario());
        destino.setNombres(origen.getNombres());
        destino.setApellidoPaterno(origen.getApellidoPaterno());
        destino.setApellidoMaterno(origen.getApellidoMaterno());
        destino.setEmail(origen.getEmail());
        destino.setHashContrasena(origen.getHashContrasena());
        destino.setRol(rol);
        destino.setActivo(origen.isActivo());
        destino.setFechaExpiracion(origen.getFechaExpiracion());
        destino.setIntentosFallidos(origen.getIntentosFallidos());
        destino.setBloqueadoHasta(origen.getBloqueadoHasta());
        destino.setDebeCambiarContrasena(origen.isDebeCambiarContrasena());
        destino.setFechaUltimoAcceso(origen.getFechaUltimoAcceso());
        destino.setCreadoPor(origen.getCreadoPor());
        destino.setModificadoPor(origen.getModificadoPor());
        destino.setFechaCreacion(origen.getFechaCreacion());
        destino.setFechaModificacion(origen.getFechaModificacion());
        destino.setFechaEliminacion(origen.getFechaEliminacion());
    }

    static TokenRefresco aDominio(TokenRefrescoEntidad entidad) {
        return new TokenRefresco(entidad.getId(), entidad.getIdUsuario(), entidad.getHashToken(),
                entidad.getFechaCreacion(), entidad.getFechaExpiracion(), entidad.getFechaRevocacion());
    }

    static void copiar(TokenRefresco origen, TokenRefrescoEntidad destino) {
        destino.setIdUsuario(origen.getIdUsuario());
        destino.setHashToken(origen.getHashToken());
        destino.setFechaCreacion(origen.getFechaCreacion());
        destino.setFechaExpiracion(origen.getFechaExpiracion());
        destino.setFechaRevocacion(origen.getFechaRevocacion());
    }
}
