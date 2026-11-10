import { Margen, ResultadoProyecto } from './resultado';
import { Naturaleza } from './movimiento';

export interface ResumenPeriodo {
  readonly desde: string;
  readonly hasta: string;
  readonly ingresos: number;
  /** Egresos imputados a proyectos. */
  readonly costosDirectos: number;
  /** Egresos sin proyecto (oficina, servicios, compras generales). */
  readonly gastosGenerales: number;
  readonly margenBruto: Margen;
  readonly resultadoNeto: number;
}

/** mes en formato AAAA-MM. */
export interface TotalMensual {
  readonly mes: string;
  readonly ingresos: number;
  readonly egresos: number;
}

export interface MovimientoDestacado {
  readonly id: number;
  readonly fecha: string;
  readonly tipo: string;
  readonly naturaleza: Naturaleza;
  readonly concepto: string;
  readonly monto: number;
  readonly proyecto: string | null;
}

export interface IndicadoresFinancieros {
  readonly actual: ResumenPeriodo;
  readonly anterior: ResumenPeriodo;
  readonly evolucion: readonly TotalMensual[];
  readonly margenProyectos: readonly ResultadoProyecto[];
  readonly cuentasPorCobrar: readonly ResultadoProyecto[];
  readonly totalPorCobrar: number;
  readonly movimientosSignificativos: readonly MovimientoDestacado[];
  readonly umbralSignificativo: number;
}

export type SentidoVariacion = 'sube' | 'baja' | 'igual';

export interface Variacion {
  readonly porcentaje: number;
  readonly sentido: SentidoVariacion;
}

const DECIMALES_VARIACION = 1;

/** Cambio porcentual frente al periodo anterior; nulo si el anterior fue cero. */
export function variacion(actual: number, anterior: number): Variacion | null {
  if (anterior === 0) {
    return null;
  }
  const porcentaje = Number((((actual - anterior) / Math.abs(anterior)) * 100).toFixed(DECIMALES_VARIACION));
  return { porcentaje: Math.abs(porcentaje), sentido: porcentaje > 0 ? 'sube' : porcentaje < 0 ? 'baja' : 'igual' };
}

/** Cantidad de proyectos a mostrar en cada extremo del ranking de margen. */
export const PROYECTOS_POR_EXTREMO = 5;

/** Los de mayor margen y, aparte, los de margen negativo o bajo que piden atencion. */
export function extremosDeMargen(
  proyectos: readonly ResultadoProyecto[],
  margenMinimo: number,
): { mejores: ResultadoProyecto[]; enAlerta: ResultadoProyecto[] } {
  const enAlerta = proyectos.filter(
    (proyecto) => proyecto.margen.porcentaje !== null && proyecto.margen.porcentaje < margenMinimo,
  );
  const mejores = proyectos.filter((proyecto) => !enAlerta.includes(proyecto)).slice(0, PROYECTOS_POR_EXTREMO);
  return { mejores, enAlerta: enAlerta.slice(-PROYECTOS_POR_EXTREMO).reverse() };
}
