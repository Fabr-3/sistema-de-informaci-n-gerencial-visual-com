import { Categoria, Fabricante } from './catalogos';

export const TIPOS_CONTROL = {
  UNIDAD: 'Por unidad',
  LOTE: 'Por lote',
} as const;

export type TipoControl = keyof typeof TIPOS_CONTROL;

export interface Especificacion {
  readonly caracteristica: string;
  readonly valor: string;
}

export interface ModeloActivo {
  readonly id: number;
  readonly nombre: string;
  readonly fabricante: Fabricante;
  readonly categoria: Categoria;
  readonly tipoControl: TipoControl;
  /** Horas de uso entre mantenimientos preventivos (solo por unidad). */
  readonly horasMantenimiento: number | null;
  /** Prefijo de los codigos de inventario de sus unidades (NOVA genera NOVA-0001). */
  readonly prefijoCodigo: string | null;
  readonly especificaciones: readonly Especificacion[];
  /** Unidades vigentes no dadas de baja (por unidad) o existencias totales (por lote). */
  readonly cantidadInventario: number;
}

export interface DatosModelo {
  readonly idFabricante: number;
  readonly idCategoria: number;
  readonly nombre: string;
  readonly tipoControl: TipoControl;
  readonly horasMantenimiento: number | null;
  readonly prefijoCodigo: string | null;
  readonly especificaciones: readonly Especificacion[];
}

export function nombreCompletoModelo(modelo: ModeloActivo): string {
  return `${modelo.fabricante.nombre} ${modelo.nombre}`;
}
