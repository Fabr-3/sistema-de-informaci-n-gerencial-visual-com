import { TipoEstadoVisual } from '../../../../compartido/modelos/estado-visual';
import { Lugar } from './catalogos';
import { ResumenCliente } from './cliente';
import { Cotizacion } from './cotizacion';

export const ESTADOS_PROYECTO = {
  PLANIFICACION: 'Planificación',
  EN_EJECUCION: 'En ejecución',
  FINALIZADO: 'Finalizado',
  CANCELADO: 'Cancelado',
} as const;

export type EstadoProyecto = keyof typeof ESTADOS_PROYECTO;

/** Transiciones permitidas (reflejan las del backend). */
export const TRANSICIONES_PROYECTO: Readonly<Record<EstadoProyecto, readonly EstadoProyecto[]>> = {
  PLANIFICACION: ['EN_EJECUCION', 'CANCELADO'],
  EN_EJECUCION: ['FINALIZADO', 'CANCELADO'],
  FINALIZADO: [],
  CANCELADO: [],
};

export function proyectoVigente(estado: EstadoProyecto): boolean {
  return estado === 'PLANIFICACION' || estado === 'EN_EJECUCION';
}

const TIPO_VISUAL_PROYECTO: Readonly<Record<EstadoProyecto, TipoEstadoVisual>> = {
  PLANIFICACION: 'alerta',
  EN_EJECUCION: 'activo',
  FINALIZADO: 'neutro',
  CANCELADO: 'peligro',
};

export function tipoVisualProyecto(estado: EstadoProyecto): TipoEstadoVisual {
  return TIPO_VISUAL_PROYECTO[estado];
}

export const ESTADOS_EVENTO = {
  PROGRAMADO: 'Programado',
  EJECUTADO: 'Ejecutado',
  CANCELADO: 'Cancelado',
} as const;

export type EstadoEvento = keyof typeof ESTADOS_EVENTO;

const TIPO_VISUAL_EVENTO: Readonly<Record<EstadoEvento, TipoEstadoVisual>> = {
  PROGRAMADO: 'alerta',
  EJECUTADO: 'activo',
  CANCELADO: 'neutro',
};

export function tipoVisualEvento(estado: EstadoEvento): TipoEstadoVisual {
  return TIPO_VISUAL_EVENTO[estado];
}

export interface Proyecto {
  readonly id: number;
  readonly idCotizacion: number;
  readonly numeroCotizacion: string;
  readonly cliente: ResumenCliente;
  readonly nombre: string;
  readonly descripcion: string | null;
  readonly fechaInicio: string | null;
  readonly fechaFin: string | null;
  readonly estado: EstadoProyecto;
  readonly fechaCreacion: string;
}

export interface Evento {
  readonly id: number;
  readonly idProyecto: number;
  readonly lugar: Lugar;
  readonly nombre: string;
  readonly fechaInicio: string;
  readonly fechaFin: string;
  readonly estado: EstadoEvento;
}

export interface Contrato {
  readonly id: number;
  readonly idProyecto: number;
  readonly numero: string;
  readonly fechaFirma: string;
  readonly terminos: string | null;
  /** Escaneo del contrato firmado; null si aun no se adjunto. */
  readonly archivoFirmado: string | null;
}

/** Tamano maximo del contrato firmado; coincide con el limite del backend. */
export const TAMANO_MAXIMO_FIRMADO = 10 * 1024 * 1024;
const TIPO_PDF = 'application/pdf';

/** Motivo por el que el archivo no sirve como contrato firmado, o null si es valido. */
export function problemaArchivoFirmado(archivo: File): string | null {
  if (archivo.type !== TIPO_PDF) {
    return 'El contrato firmado debe ser un archivo PDF';
  }
  if (archivo.size > TAMANO_MAXIMO_FIRMADO) {
    return 'El contrato firmado no puede superar los 10 MB';
  }
  return null;
}

export interface DetalleProyecto {
  readonly proyecto: Proyecto;
  readonly cotizacion: Cotizacion;
  readonly eventos: readonly Evento[];
  readonly contratos: readonly Contrato[];
}

/** Evento del cronograma general con su proyecto y cliente. */
export interface EventoCalendario {
  readonly id: number;
  readonly nombre: string;
  readonly fechaInicio: string;
  readonly fechaFin: string;
  readonly estado: EstadoEvento;
  readonly idProyecto: number;
  readonly proyecto: string;
  readonly cliente: string;
  readonly lugar: string;
  readonly ciudad: string | null;
}

export interface DatosProyecto {
  readonly nombre: string;
  readonly descripcion: string | null;
  readonly fechaInicio: string | null;
  readonly fechaFin: string | null;
}

export interface DatosEvento {
  readonly idLugar: number;
  readonly nombre: string;
  /** Fecha y hora local ISO (yyyy-MM-ddTHH:mm). */
  readonly fechaInicio: string;
  readonly fechaFin: string;
}

export interface DatosContrato {
  readonly fechaFirma: string;
  readonly terminos: string | null;
}

export interface FiltroProyectos {
  readonly texto?: string;
  readonly estado?: EstadoProyecto;
  readonly idCliente?: number;
}
