import { CodigoRol } from './rol';

export interface UsuarioSesion {
  readonly id: number;
  readonly nombreUsuario: string;
  readonly nombreCompleto: string;
  readonly email: string;
  readonly codigoRol: CodigoRol;
  readonly nombreRol: string;
  readonly permisos: readonly string[];
  readonly debeCambiarContrasena: boolean;
}

/**
 * Sesion activa. El token de acceso se mantiene solo en memoria (no en
 * localStorage) para reducir el impacto de un XSS; la sesion se recupera al
 * recargar mediante la cookie HttpOnly de refresco.
 */
export interface Sesion {
  readonly tokenAcceso: string;
  readonly expiraEn: Date;
  readonly usuario: UsuarioSesion;
}

export interface Credenciales {
  readonly nombreUsuario: string;
  readonly contrasena: string;
}

export interface CambioContrasena {
  readonly contrasenaActual: string;
  readonly nuevaContrasena: string;
}
