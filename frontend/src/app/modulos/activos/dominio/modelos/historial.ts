import { TipoEstadoVisual } from '../../../../compartido/modelos/estado-visual';
import { TipoMantenimiento } from './catalogos';

export interface RegistroUso {
  readonly id: number;
  readonly idActivo: number;
  readonly fechaInicio: string;
  readonly fechaFin: string;
  readonly descripcion: string | null;
  readonly nombreCreador: string | null;
  readonly fechaCreacion: string;
}

export interface NuevoRegistroUso {
  /** Fecha y hora local ISO (yyyy-MM-ddTHH:mm). */
  readonly fechaInicio: string;
  readonly fechaFin: string;
  readonly descripcion: string | null;
}

export const CONDICIONES_FINALES = {
  OPERATIVO: 'Operativo',
  OPERATIVO_CON_OBSERVACIONES: 'Operativo con observaciones',
  NO_REPARABLE: 'No reparable (se da de baja)',
} as const;

export type CondicionFinal = keyof typeof CONDICIONES_FINALES;

export interface Mantenimiento {
  readonly id: number;
  readonly idActivo: number;
  readonly tipo: TipoMantenimiento;
  readonly fechaInicio: string;
  readonly fechaFin: string | null;
  readonly descripcion: string | null;
  readonly observaciones: string | null;
  readonly condicionFinal: CondicionFinal | null;
  readonly costo: number | null;
  readonly estado: 'EN_PROCESO' | 'FINALIZADO';
}

export interface InicioMantenimiento {
  readonly idTipoMantenimiento: number;
  readonly fechaInicio: string;
  readonly descripcion: string | null;
}

export interface FinMantenimiento {
  readonly fechaFin: string;
  readonly condicionFinal: CondicionFinal;
  readonly observaciones: string | null;
  readonly costo: number | null;
}

export const ESTADOS_MODULO = {
  ACTIVO: 'Activo',
  DEFECTUOSO: 'Defectuoso',
  RETIRADO: 'Retirado',
} as const;

export type EstadoModuloLed = keyof typeof ESTADOS_MODULO;

export interface ModuloLed {
  readonly id: number;
  readonly idActivoPadre: number;
  readonly codigo: string;
  readonly estado: EstadoModuloLed;
  readonly fechaCreacion: string;
}

const TIPO_VISUAL_MODULO: Readonly<Record<EstadoModuloLed, TipoEstadoVisual>> = {
  ACTIVO: 'activo',
  DEFECTUOSO: 'peligro',
  RETIRADO: 'neutro',
};

export function tipoVisualModulo(estado: EstadoModuloLed): TipoEstadoVisual {
  return TIPO_VISUAL_MODULO[estado];
}

/** Horas entre dos fechas ISO locales, con dos decimales. */
export function horasEntre(inicio: string, fin: string): number {
  const milisegundos = new Date(fin).getTime() - new Date(inicio).getTime();
  return Math.round((milisegundos / 3_600_000) * 100) / 100;
}
