import { CodigoRol } from './rol';

export interface Usuario {
  readonly id: number;
  readonly nombreUsuario: string;
  readonly nombres: string;
  readonly apellidoPaterno: string;
  readonly apellidoMaterno: string | null;
  readonly nombreCompleto: string;
  readonly email: string;
  readonly idRol: number;
  readonly codigoRol: CodigoRol;
  readonly nombreRol: string;
  readonly activo: boolean;
  readonly bloqueado: boolean;
  readonly expirado: boolean;
  /** Fecha y hora local ISO (sin zona horaria). */
  readonly fechaExpiracion: string | null;
  readonly debeCambiarContrasena: boolean;
  readonly fechaUltimoAcceso: string | null;
  readonly fechaCreacion: string;
}

export interface FiltroUsuarios {
  readonly texto?: string;
  readonly idRol?: number;
  readonly activo?: boolean;
}

export interface DatosUsuario {
  readonly nombres: string;
  readonly apellidoPaterno: string;
  readonly apellidoMaterno: string | null;
  readonly email: string;
  readonly idRol: number;
  readonly fechaExpiracion: string | null;
}

export interface NuevoUsuario extends DatosUsuario {
  readonly nombreUsuario: string;
  readonly contrasenaTemporal: string;
}

export type TipoEstadoUsuario = 'activo' | 'alerta' | 'peligro' | 'neutro';

export interface EstadoUsuario {
  readonly etiqueta: string;
  readonly tipo: TipoEstadoUsuario;
}

/**
 * Estado visible de la cuenta. Prioridad: inactiva, expirada, bloqueada,
 * pendiente de cambio de contrasena y, por ultimo, activa.
 */
export function estadoUsuario(usuario: Usuario): EstadoUsuario {
  if (!usuario.activo) {
    return { etiqueta: 'Inactivo', tipo: 'neutro' };
  }
  if (usuario.expirado) {
    return { etiqueta: 'Expirado', tipo: 'peligro' };
  }
  if (usuario.bloqueado) {
    return { etiqueta: 'Bloqueado', tipo: 'peligro' };
  }
  if (usuario.debeCambiarContrasena) {
    return { etiqueta: 'Contraseña temporal', tipo: 'alerta' };
  }
  return { etiqueta: 'Activo', tipo: 'activo' };
}
