import { Sesion, UsuarioSesion } from '../dominio/modelos/sesion';
import { Usuario } from '../dominio/modelos/usuario';

/** Datos de prueba reutilizables del modulo de accesos. */
export function usuarioSesion(cambios: Partial<UsuarioSesion> = {}): UsuarioSesion {
  return {
    id: 1,
    nombreUsuario: 'admin',
    nombreCompleto: 'Administrador Sistema',
    email: 'admin@visual.com.bo',
    codigoRol: 'ADMINISTRADOR',
    nombreRol: 'Administrador',
    permisos: ['USUARIOS_VER', 'USUARIOS_GESTIONAR'],
    debeCambiarContrasena: false,
    ...cambios,
  };
}

export function sesion(token = 'token-1', cambios: Partial<UsuarioSesion> = {}): Sesion {
  return { tokenAcceso: token, expiraEn: new Date('2026-11-08T12:00:00Z'), usuario: usuarioSesion(cambios) };
}

export function usuario(cambios: Partial<Usuario> = {}): Usuario {
  return {
    id: 2,
    nombreUsuario: 'jperez',
    nombres: 'Juan',
    apellidoPaterno: 'Perez',
    apellidoMaterno: null,
    nombreCompleto: 'Juan Perez',
    email: 'jperez@visual.com.bo',
    idRol: 3,
    codigoRol: 'TECNICO',
    nombreRol: 'Tecnico',
    activo: true,
    bloqueado: false,
    expirado: false,
    fechaExpiracion: null,
    debeCambiarContrasena: false,
    fechaUltimoAcceso: null,
    fechaCreacion: '2026-11-08T10:00:00',
    ...cambios,
  };
}
