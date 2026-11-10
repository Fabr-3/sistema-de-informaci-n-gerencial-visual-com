import { Pagina } from '../../../../compartido/modelos/pagina';
import { TipoEstadoVisual } from '../../../../compartido/modelos/estado-visual';

export const NATURALEZAS = {
  INGRESO: 'Ingreso',
  EGRESO: 'Egreso',
} as const;

export type Naturaleza = keyof typeof NATURALEZAS;

export const ESTADOS_MOVIMIENTO = {
  REGISTRADO: 'Registrado',
  ANULADO: 'Anulado',
} as const;

export type EstadoMovimiento = keyof typeof ESTADOS_MOVIMIENTO;

export interface TipoMovimiento {
  readonly id: number;
  readonly nombre: string;
  readonly naturaleza: Naturaleza;
  readonly requiereProveedor: boolean;
  readonly descuentaInventario: boolean;
}

/** Proyecto, evento o proveedor imputado, con su nombre visible. */
export interface Referencia {
  readonly id: number;
  readonly nombre: string;
}

export interface ItemVenta {
  readonly idActivo: number | null;
  readonly idExistencia: number | null;
  readonly cantidad: number;
  readonly descripcion: string | null;
}

export interface Movimiento {
  readonly id: number;
  readonly tipo: TipoMovimiento;
  readonly concepto: string;
  readonly monto: number;
  readonly fecha: string;
  readonly proyecto: Referencia | null;
  readonly evento: Referencia | null;
  readonly proveedor: Referencia | null;
  readonly numeroFactura: string | null;
  readonly estado: EstadoMovimiento;
  readonly motivoAnulacion: string | null;
  readonly items: readonly ItemVenta[];
  readonly nombreCreador: string | null;
  readonly fechaCreacion: string;
}

/** Pagina de movimientos con los totales (no anulados) de todo el filtro. */
export interface ListadoMovimientos {
  readonly pagina: Pagina<Movimiento>;
  readonly ingresos: number;
  readonly egresos: number;
  readonly saldo: number;
}

export interface ItemNuevaVenta {
  readonly idActivo?: number;
  readonly idExistencia?: number;
  readonly cantidad?: number;
}

export interface DatosMovimiento {
  readonly idTipo: number;
  readonly concepto: string;
  readonly monto: number;
  readonly fecha: string;
  readonly idProyecto: number | null;
  readonly idEvento: number | null;
  readonly idProveedor: number | null;
  readonly numeroFactura: string | null;
  readonly items: readonly ItemNuevaVenta[];
}

/** Fechas ISO (AAAA-MM-DD) incluidas. */
export interface FiltroMovimientos {
  readonly desde?: string;
  readonly hasta?: string;
  readonly naturaleza?: Naturaleza;
  readonly idTipo?: number;
  readonly idProyecto?: number;
  readonly estado?: EstadoMovimiento;
  readonly texto?: string;
}

/** Positivo si ingresa dinero, negativo si sale. */
export function montoConSigno(movimiento: Movimiento): number {
  return movimiento.tipo.naturaleza === 'INGRESO' ? movimiento.monto : -movimiento.monto;
}

export function tipoVisualNaturaleza(naturaleza: Naturaleza): TipoEstadoVisual {
  return naturaleza === 'INGRESO' ? 'activo' : 'peligro';
}

export function tiposDe(tipos: readonly TipoMovimiento[], naturaleza: Naturaleza): TipoMovimiento[] {
  return tipos.filter((tipo) => tipo.naturaleza === naturaleza);
}
