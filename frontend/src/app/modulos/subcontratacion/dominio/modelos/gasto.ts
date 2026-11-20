import { ElementoCatalogo } from '../../../comercial/dominio/modelos/catalogos';
import { EstadoConsumo } from './consumo';

/** Gasto que el tecnico hace en un evento (fuera del catalogo de consumibles), con su comprobante. */
export interface GastoEvento {
  readonly id: number;
  readonly proyecto: ElementoCatalogo;
  readonly evento: ElementoCatalogo;
  readonly descripcion: string;
  readonly monto: number;
  readonly fecha: string;
  /** Nombre original del archivo del comprobante. */
  readonly comprobante: string;
  readonly tipoComprobante: string;
  readonly estado: EstadoConsumo;
  readonly motivoAnulacion: string | null;
  readonly idMovimiento: number | null;
  readonly registradoPor: string | null;
}

export interface DatosGasto {
  readonly idProyecto: number;
  readonly idEvento: number;
  readonly descripcion: string;
  readonly monto: number;
  readonly fecha: string;
}

export interface FiltroGastos {
  readonly idProyecto?: number;
  readonly idEvento?: number;
  readonly desde?: string;
  readonly hasta?: string;
}

/** Tipos de comprobante aceptados (atributo accept del selector de archivos). */
export const TIPOS_COMPROBANTE = ['image/jpeg', 'image/png', 'image/webp', 'application/pdf'] as const;
export const TAMANO_MAXIMO_COMPROBANTE = 5 * 1024 * 1024;

/** Motivo por el que el archivo no sirve como comprobante, o null si es valido. */
export function problemaComprobante(archivo: { readonly type: string; readonly size: number }): string | null {
  if (!(TIPOS_COMPROBANTE as readonly string[]).includes(archivo.type)) {
    return 'El comprobante debe ser una foto (JPG, PNG o WEBP) o un PDF';
  }
  if (archivo.size === 0) {
    return 'El archivo está vacío';
  }
  return archivo.size > TAMANO_MAXIMO_COMPROBANTE ? 'El comprobante no puede superar los 5 MB' : null;
}

/** Monto de los gastos registrados (los anulados no cuentan). */
export function montoRegistrado(gastos: readonly GastoEvento[]): number {
  return gastos.filter((gasto) => gasto.estado === 'REGISTRADO').reduce((suma, gasto) => suma + gasto.monto, 0);
}
