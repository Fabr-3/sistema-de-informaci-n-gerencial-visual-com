import { ElementoCatalogo } from '../../../comercial/dominio/modelos/catalogos';

export const ESTADOS_CONSUMO = {
  REGISTRADO: 'Registrado',
  ANULADO: 'Anulado',
} as const;

export type EstadoConsumo = keyof typeof ESTADOS_CONSUMO;

/** Consumible usado en un proyecto o evento, con el costo copiado al registrarse. */
export interface Consumo {
  readonly id: number;
  readonly consumible: ElementoCatalogo;
  readonly unidadMedida: string;
  readonly proyecto: ElementoCatalogo;
  readonly evento: ElementoCatalogo | null;
  readonly cantidad: number;
  readonly costoUnitario: number;
  readonly costoTotal: number;
  readonly fecha: string;
  readonly estado: EstadoConsumo;
  readonly motivoAnulacion: string | null;
  readonly registradoPor: string | null;
}

export interface DatosConsumo {
  readonly idConsumible: number;
  readonly idProyecto: number;
  readonly idEvento: number | null;
  readonly cantidad: number;
  readonly fecha: string;
}

export interface FiltroConsumos {
  readonly idProyecto?: number;
  readonly idConsumible?: number;
  readonly desde?: string;
  readonly hasta?: string;
}

/** Evento donde trabaja el tecnico (solo puede registrar consumos ahi). */
export interface EventoAsignado {
  readonly idEvento: number;
  readonly evento: string;
  readonly idProyecto: number;
  readonly proyecto: string;
  readonly fechaInicio: string;
}

/** Costo total de los consumos registrados (los anulados no cuentan). */
export function costoRegistrado(consumos: readonly Consumo[]): number {
  return consumos
    .filter((consumo) => consumo.estado === 'REGISTRADO')
    .reduce((total, consumo) => total + consumo.costoTotal, 0);
}
