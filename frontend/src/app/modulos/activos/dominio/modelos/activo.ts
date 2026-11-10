import { TipoEstadoVisual } from '../../../../compartido/modelos/estado-visual';
import { Especificacion } from './modelo-activo';

export const ESTADOS_OPERATIVOS = {
  DISPONIBLE: 'Disponible',
  RESERVADO: 'Reservado',
  EN_USO: 'En uso',
  EN_MANTENIMIENTO: 'En mantenimiento',
  BAJA: 'Dado de baja',
  VENDIDO: 'Vendido',
} as const;

export type EstadoOperativo = keyof typeof ESTADOS_OPERATIVOS;

export const NIVELES_ALERTA = {
  SIN_PLAN: 'Sin plan',
  AL_DIA: 'Al día',
  PROXIMO: 'Próximo',
  VENCIDO: 'Vencido',
} as const;

export type NivelAlertaMantenimiento = keyof typeof NIVELES_ALERTA;

export interface Activo {
  readonly id: number;
  readonly codigoInventario: string;
  readonly numeroSerie: string | null;
  readonly idModelo: number;
  readonly modelo: string;
  readonly idCategoria: number;
  readonly categoria: string;
  readonly idBodega: number;
  readonly bodega: string;
  readonly estado: EstadoOperativo;
  readonly fechaAdquisicion: string | null;
  readonly costoAdquisicion: number | null;
  readonly observaciones: string | null;
  readonly motivoBaja: string | null;
  readonly horasTotales: number;
  readonly horasDesdeMantenimiento: number;
  readonly horasMantenimiento: number | null;
  readonly fechaUltimoMantenimiento: string | null;
  readonly nivelAlerta: NivelAlertaMantenimiento;
  readonly especificaciones: readonly Especificacion[];
  readonly fechaCreacion: string;
}

/** Edicion de un activo (el codigo de inventario no cambia). */
export interface DatosActivo {
  readonly numeroSerie: string | null;
  readonly idModelo: number;
  readonly idBodega: number;
  /** Fecha ISO (yyyy-MM-dd). */
  readonly fechaAdquisicion: string | null;
  readonly costoAdquisicion: number | null;
  readonly observaciones: string | null;
}

/**
 * Registro de una o varias unidades de un modelo. Los codigos se generan con
 * el prefijo del modelo; el codigo manual solo aplica a una unidad.
 */
export interface RegistroActivos {
  readonly idModelo: number;
  readonly idBodega: number;
  readonly cantidad: number;
  readonly codigoInventario: string | null;
  /** Una serie por unidad, o vacio. */
  readonly numerosSerie: readonly string[];
  readonly fechaAdquisicion: string | null;
  readonly costoUnitario: number | null;
  readonly observaciones: string | null;
}

export const CANTIDAD_MAXIMA_REGISTRO = 100;

export interface FiltroActivos {
  readonly texto?: string;
  readonly idCategoria?: number;
  readonly idModelo?: number;
  readonly idBodega?: number;
  readonly estado?: EstadoOperativo;
  readonly soloAlertas?: boolean;
}

const TIPO_VISUAL_ESTADO: Readonly<Record<EstadoOperativo, TipoEstadoVisual>> = {
  DISPONIBLE: 'activo',
  RESERVADO: 'neutro',
  EN_USO: 'neutro',
  EN_MANTENIMIENTO: 'alerta',
  BAJA: 'peligro',
  VENDIDO: 'neutro',
};

const TIPO_VISUAL_ALERTA: Readonly<Record<NivelAlertaMantenimiento, TipoEstadoVisual>> = {
  SIN_PLAN: 'neutro',
  AL_DIA: 'activo',
  PROXIMO: 'alerta',
  VENCIDO: 'peligro',
};

export function tipoVisualEstado(estado: EstadoOperativo): TipoEstadoVisual {
  return TIPO_VISUAL_ESTADO[estado];
}

export function tipoVisualAlerta(nivel: NivelAlertaMantenimiento): TipoEstadoVisual {
  return TIPO_VISUAL_ALERTA[nivel];
}

/** Porcentaje del intervalo de mantenimiento consumido (null si no hay plan). */
export function avanceMantenimiento(activo: Activo): number | null {
  if (!activo.horasMantenimiento) {
    return null;
  }
  const avance = (activo.horasDesdeMantenimiento / activo.horasMantenimiento) * 100;
  return Math.min(Math.round(avance), 100);
}

/** Dado de baja o vendido: ya no forma parte del inventario. */
export function fueraDeInventario(estado: EstadoOperativo): boolean {
  return estado === 'BAJA' || estado === 'VENDIDO';
}

/** Reglas de interfaz que reflejan las del backend (el backend siempre valida). */
export const reglasActivo = {
  puedeRegistrarUso: (activo: Activo): boolean =>
    !fueraDeInventario(activo.estado) && activo.estado !== 'EN_MANTENIMIENTO',
  puedeIniciarMantenimiento: (activo: Activo): boolean => activo.estado === 'DISPONIBLE',
  /** En mantenimiento se da de baja al finalizarlo como no reparable. */
  puedeDarDeBaja: (activo: Activo): boolean => activo.estado === 'DISPONIBLE',
};
