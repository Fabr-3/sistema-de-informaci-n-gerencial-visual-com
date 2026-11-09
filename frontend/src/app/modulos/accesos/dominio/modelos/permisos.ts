/** Codigos de permisos fijos del sistema (coinciden con el backend). */
export const Permisos = {
  USUARIOS_VER: 'USUARIOS_VER',
  USUARIOS_GESTIONAR: 'USUARIOS_GESTIONAR',
  ROLES_VER: 'ROLES_VER',
  ROLES_GESTIONAR: 'ROLES_GESTIONAR',
  AUDITORIA_VER: 'AUDITORIA_VER',
  RESPALDOS_GESTIONAR: 'RESPALDOS_GESTIONAR',
  ACTIVOS_VER: 'ACTIVOS_VER',
  ACTIVOS_GESTIONAR: 'ACTIVOS_GESTIONAR',
  MANTENIMIENTOS_REGISTRAR: 'MANTENIMIENTOS_REGISTRAR',
} as const;

export type CodigoPermiso = (typeof Permisos)[keyof typeof Permisos];

export interface Permiso {
  readonly id: number;
  readonly modulo: string;
  readonly nombre: string;
  readonly descripcion: string | null;
}

/** Nombres visibles de los modulos funcionales que agrupan los permisos. */
export const NOMBRES_MODULOS: Readonly<Record<string, string>> = {
  ACCESOS: 'Administración de accesos',
  ACTIVOS: 'Activos e inventario',
  COMERCIAL: 'Comercial y planificación',
  LOGISTICA: 'Logística',
  FINANZAS: 'Financiero',
  GERENCIAL: 'Información gerencial',
  PROVEEDORES: 'Subcontratación y consumibles',
};
