import { TipoEstadoVisual } from '../../../../compartido/modelos/estado-visual';
import { ResumenCliente } from './cliente';

export const ESTADOS_COTIZACION = {
  BORRADOR: 'Borrador',
  ACEPTADA: 'Aceptada',
  RECHAZADA: 'Rechazada',
  VENCIDA: 'Vencida',
} as const;

export type EstadoCotizacion = keyof typeof ESTADOS_COTIZACION;

const TIPO_VISUAL: Readonly<Record<EstadoCotizacion, TipoEstadoVisual>> = {
  BORRADOR: 'neutro',
  ACEPTADA: 'activo',
  RECHAZADA: 'peligro',
  VENCIDA: 'alerta',
};

export function tipoVisualCotizacion(estado: EstadoCotizacion): TipoEstadoVisual {
  return TIPO_VISUAL[estado];
}

export interface LineaCotizacion {
  readonly idServicio: number;
  readonly servicio: string;
  readonly unidadMedida: string;
  readonly cantidad: number;
  readonly precioUnitario: number;
  readonly descuento: number;
  readonly subtotal: number;
}

export interface Cotizacion {
  readonly id: number;
  readonly numero: string;
  readonly cliente: ResumenCliente;
  readonly titulo: string;
  readonly fechaEmision: string;
  readonly fechaValidez: string;
  readonly fechaEventoInicio: string | null;
  readonly fechaEventoFin: string | null;
  readonly estado: EstadoCotizacion;
  readonly condiciones: string | null;
  readonly lineas: readonly LineaCotizacion[];
  readonly importeTotal: number;
  readonly resueltoPor: string | null;
  readonly fechaResolucion: string | null;
  readonly motivoRechazo: string | null;
  /** Proyecto generado al aceptarse. */
  readonly idProyecto: number | null;
  readonly fechaCreacion: string;
}

export interface DatosLinea {
  readonly idServicio: number;
  readonly cantidad: number;
  readonly precioUnitario: number;
  readonly descuento: number;
}

export interface DatosCotizacion {
  readonly idCliente: number;
  readonly titulo: string;
  /** Fechas ISO (yyyy-MM-dd). */
  readonly fechaEmision: string;
  readonly fechaValidez: string;
  readonly fechaEventoInicio: string | null;
  readonly fechaEventoFin: string | null;
  readonly condiciones: string | null;
  readonly lineas: readonly DatosLinea[];
}

export interface FiltroCotizaciones {
  readonly texto?: string;
  readonly estado?: EstadoCotizacion;
  readonly idCliente?: number;
}

/** Subtotal de una linea (cantidad x precio - descuento); el backend recalcula siempre. */
export function subtotalLinea(linea: DatosLinea): number {
  return redondear(linea.cantidad * linea.precioUnitario - linea.descuento);
}

export function totalCotizacion(lineas: readonly DatosLinea[]): number {
  return redondear(lineas.reduce((total, linea) => total + subtotalLinea(linea), 0));
}

function redondear(valor: number): number {
  return Math.round(valor * 100) / 100;
}
