import { TipoEstadoVisual } from '../../../../compartido/modelos/estado-visual';
import { EstadoProyecto } from '../../../comercial/dominio/modelos/proyecto';
import { Naturaleza } from './movimiento';

/** Margen = ingresos - costos directos; porcentaje sobre ingresos (nulo sin ingresos). */
export interface Margen {
  readonly ingresos: number;
  readonly costos: number;
  readonly margen: number;
  readonly porcentaje: number | null;
}

export interface ResultadoProyecto {
  readonly idProyecto: number;
  readonly proyecto: string;
  readonly cliente: string;
  readonly estado: EstadoProyecto;
  readonly contratado: number;
  readonly margen: Margen;
  readonly porCobrar: number;
}

/** idEvento nulo: lo imputado al proyecto sin evento. */
export interface ResultadoEvento {
  readonly idEvento: number | null;
  readonly evento: string;
  readonly fechaInicio: string | null;
  readonly margen: Margen;
}

export interface TotalPorTipo {
  readonly tipo: string;
  readonly naturaleza: Naturaleza;
  readonly monto: number;
}

export interface DetalleResultadoProyecto {
  readonly proyecto: ResultadoProyecto;
  readonly eventos: readonly ResultadoEvento[];
  readonly porTipo: readonly TotalPorTipo[];
}

export const ORDENES_RESULTADOS = {
  FECHA_DESC: 'Más recientes',
  MARGEN_DESC: 'Mayor margen',
  MARGEN_ASC: 'Menor margen',
  MARGEN_PORCENTAJE_DESC: 'Mayor margen porcentual',
} as const;

export type OrdenResultados = keyof typeof ORDENES_RESULTADOS;

/** desde y hasta (AAAA-MM-DD, incluidos) toman los proyectos cuyo periodo se superpone con el rango. */
export interface FiltroResultados {
  readonly texto?: string;
  readonly estado?: EstadoProyecto;
  readonly desde?: string;
  readonly hasta?: string;
  readonly idCliente?: number;
  readonly orden?: OrdenResultados;
}

/** El rango solo es valido si la fecha inicial no es posterior a la final (o falta alguna). */
export function rangoValido(desde?: string, hasta?: string): boolean {
  return !desde || !hasta || desde <= hasta;
}

/** Por debajo de este porcentaje el margen se muestra como alerta. */
export const MARGEN_MINIMO_SALUDABLE = 20;

/** Sin ingresos no hay margen que evaluar (p. ej. un evento cuyos cobros se imputaron al proyecto). */
export function tipoVisualMargen(margen: Margen): TipoEstadoVisual {
  if (margen.porcentaje === null) {
    return 'neutro';
  }
  if (margen.porcentaje < 0) {
    return 'peligro';
  }
  return margen.porcentaje < MARGEN_MINIMO_SALUDABLE ? 'alerta' : 'activo';
}

/** "68,1 %" o "Sin ingresos". */
export function textoPorcentaje(margen: Margen): string {
  return margen.porcentaje === null ? 'Sin ingresos' : `${margen.porcentaje.toLocaleString('es-BO')} %`;
}
