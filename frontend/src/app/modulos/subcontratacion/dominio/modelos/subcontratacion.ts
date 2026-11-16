import { TipoEstadoVisual } from '../../../../compartido/modelos/estado-visual';
import { ElementoCatalogo } from '../../../comercial/dominio/modelos/catalogos';

export const ESTADOS_SUBCONTRATACION = {
  REGISTRADA: 'Registrada',
  ANULADA: 'Anulada',
} as const;

export type EstadoSubcontratacion = keyof typeof ESTADOS_SUBCONTRATACION;

/** Servicio contratado a un proveedor para un proyecto; su costo es un egreso financiero vinculado. */
export interface Subcontratacion {
  readonly id: number;
  readonly proveedor: ElementoCatalogo;
  readonly servicio: ElementoCatalogo | null;
  readonly proyecto: ElementoCatalogo;
  readonly evento: ElementoCatalogo | null;
  readonly descripcion: string;
  readonly costo: number;
  readonly fecha: string;
  readonly estado: EstadoSubcontratacion;
  readonly motivoAnulacion: string | null;
  readonly idMovimiento: number | null;
}

export interface DatosSubcontratacion {
  readonly idProveedor: number;
  readonly idServicio: number | null;
  readonly idProyecto: number;
  readonly idEvento: number | null;
  readonly descripcion: string;
  readonly costo: number;
  readonly fecha: string;
}

export interface FiltroSubcontrataciones {
  readonly idProyecto?: number;
  readonly idProveedor?: number;
  readonly desde?: string;
  readonly hasta?: string;
}

const TIPO_VISUAL: Readonly<Record<EstadoSubcontratacion, TipoEstadoVisual>> = {
  REGISTRADA: 'activo',
  ANULADA: 'neutro',
};

export function tipoVisualSubcontratacion(estado: EstadoSubcontratacion): TipoEstadoVisual {
  return TIPO_VISUAL[estado];
}

/** Suma de las subcontrataciones vigentes (las anuladas no cuentan). */
export function totalVigente(subcontrataciones: readonly Subcontratacion[]): number {
  return subcontrataciones
    .filter((subcontratacion) => subcontratacion.estado === 'REGISTRADA')
    .reduce((total, subcontratacion) => total + subcontratacion.costo, 0);
}
