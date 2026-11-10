export const TIPOS_MOVIMIENTO = {
  INGRESO: 'Ingreso',
  SALIDA: 'Salida',
  RETORNO: 'Retorno',
  BAJA: 'Baja',
  AJUSTE: 'Ajuste',
  TRASLADO_SALIDA: 'Traslado (salida)',
  TRASLADO_ENTRADA: 'Traslado (entrada)',
  VENTA: 'Venta',
  ANULACION_VENTA: 'Venta anulada',
} as const;

export type TipoMovimientoLote = keyof typeof TIPOS_MOVIMIENTO;

/** Tipos que se registran directamente (los traslados tienen su propia operacion). */
export const TIPOS_MOVIMIENTO_DIRECTO: readonly TipoMovimientoLote[] = [
  'INGRESO',
  'SALIDA',
  'RETORNO',
  'BAJA',
  'AJUSTE',
];

export interface Existencia {
  readonly id: number;
  readonly idModelo: number;
  readonly modelo: string;
  readonly categoria: string;
  readonly idBodega: number;
  readonly bodega: string;
  readonly cantidad: number;
}

export interface MovimientoLote {
  readonly id: number;
  readonly idExistencia: number;
  readonly tipo: TipoMovimientoLote;
  /** Con signo: positiva incrementa la existencia. */
  readonly cantidad: number;
  readonly motivo: string | null;
  readonly fecha: string;
  readonly nombreCreador: string | null;
}

export interface NuevoMovimientoLote {
  readonly idModelo: number;
  readonly idBodega: number;
  readonly tipo: TipoMovimientoLote;
  readonly cantidad: number;
  readonly motivo: string | null;
}

export interface TrasladoLote {
  readonly idModelo: number;
  readonly idBodegaOrigen: number;
  readonly idBodegaDestino: number;
  readonly cantidad: number;
  readonly motivo: string | null;
}

export interface FiltroExistencias {
  readonly texto?: string;
  readonly idCategoria?: number;
  readonly idBodega?: number;
}
