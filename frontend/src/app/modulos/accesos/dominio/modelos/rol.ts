import { Permiso } from './permisos';

export const CodigosRol = {
  ADMINISTRADOR: 'ADMINISTRADOR',
  GERENTE: 'GERENTE',
  TECNICO: 'TECNICO',
  SUBCONTRATADO_TEMPORAL: 'SUBCONTRATADO_TEMPORAL',
} as const;

export type CodigoRol = (typeof CodigosRol)[keyof typeof CodigosRol];

export interface Rol {
  readonly id: number;
  readonly codigo: CodigoRol;
  readonly nombre: string;
  readonly descripcion: string | null;
  /** El rol Administrador conserva siempre todos los permisos. */
  readonly modificable: boolean;
  readonly permisos: readonly Permiso[];
}

/** Regla de negocio: los sub-contratados temporales requieren fecha de expiracion. */
export function requiereFechaExpiracion(codigo: CodigoRol | undefined): boolean {
  return codigo === CodigosRol.SUBCONTRATADO_TEMPORAL;
}
