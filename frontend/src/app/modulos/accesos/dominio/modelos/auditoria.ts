export const ACCIONES_AUDITORIA = {
  INICIO_SESION: 'Inicio de sesión',
  INICIO_SESION_FALLIDO: 'Inicio de sesión fallido',
  CIERRE_SESION: 'Cierre de sesión',
  BLOQUEO_CUENTA: 'Bloqueo de cuenta',
  CAMBIO_CONTRASENA: 'Cambio de contraseña',
  CREAR: 'Creación',
  ACTUALIZAR: 'Actualización',
  ELIMINAR: 'Eliminación',
  RESPALDO_CREADO: 'Respaldo generado',
  RESPALDO_DESCARGADO: 'Respaldo descargado',
} as const;

export type AccionAuditoria = keyof typeof ACCIONES_AUDITORIA;

/** Acciones que indican un posible problema de seguridad. */
const ACCIONES_CRITICAS: ReadonlySet<AccionAuditoria> = new Set([
  'INICIO_SESION_FALLIDO',
  'BLOQUEO_CUENTA',
  'ELIMINAR',
]);

export function etiquetaAccion(accion: AccionAuditoria): string {
  return ACCIONES_AUDITORIA[accion] ?? accion;
}

export function esAccionCritica(accion: AccionAuditoria): boolean {
  return ACCIONES_CRITICAS.has(accion);
}

export interface RegistroAuditoria {
  readonly id: number;
  readonly fecha: string;
  readonly idUsuario: number | null;
  readonly nombreUsuario: string | null;
  readonly accion: AccionAuditoria;
  readonly entidad: string | null;
  readonly idEntidad: string | null;
  readonly valoresAnteriores: Record<string, unknown> | null;
  readonly valoresNuevos: Record<string, unknown> | null;
  readonly detalle: string | null;
  readonly direccionIp: string | null;
  readonly agenteUsuario: string | null;
}

export interface FiltroAuditoria {
  readonly desde?: string;
  readonly hasta?: string;
  readonly accion?: AccionAuditoria;
  readonly entidad?: string;
}
