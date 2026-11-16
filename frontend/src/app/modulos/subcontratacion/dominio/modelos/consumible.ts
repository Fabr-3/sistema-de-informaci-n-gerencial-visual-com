/** Material que se gasta en los eventos, con su stock y costo vigente. */
export interface Consumible {
  readonly id: number;
  readonly nombre: string;
  readonly unidadMedida: string;
  readonly stockActual: number;
  readonly stockMinimo: number | null;
  readonly costoUnitario: number;
  readonly activo: boolean;
  readonly stockBajo: boolean;
}

export interface DatosConsumible {
  readonly nombre: string;
  readonly unidadMedida: string;
  readonly stockMinimo: number | null;
  readonly costoUnitario: number;
  readonly activo: boolean;
}

export interface IngresoConsumible {
  readonly cantidad: number;
  readonly costoUnitario: number | null;
  readonly motivo: string | null;
}

/** cantidad lleva signo: positiva suma al stock y negativa lo reduce. */
export interface AjusteConsumible {
  readonly cantidad: number;
  readonly motivo: string;
}

export const TIPOS_MOVIMIENTO_CONSUMIBLE = {
  INGRESO: 'Ingreso',
  CONSUMO: 'Consumo',
  AJUSTE: 'Ajuste',
  ANULACION: 'Anulación de consumo',
} as const;

export type TipoMovimientoConsumible = keyof typeof TIPOS_MOVIMIENTO_CONSUMIBLE;

export interface MovimientoConsumible {
  readonly id: number;
  readonly tipo: TipoMovimientoConsumible;
  readonly cantidad: number;
  readonly fecha: string;
  readonly motivo: string | null;
  readonly nombreCreador: string | null;
}

export interface FiltroConsumibles {
  readonly texto?: string;
  readonly activo?: boolean;
  readonly soloStockBajo?: boolean;
}

/** Stock resultante de un ajuste; negativo indica que el backend lo rechazara. */
export function stockTrasAjuste(consumible: Consumible, cantidad: number): number {
  return Math.round((consumible.stockActual + cantidad) * 100) / 100;
}
