/** KPIs financieros de un periodo; los margenes son nulos si no hubo ingresos. */
export interface IndicadoresPeriodo {
  readonly desde: string;
  readonly hasta: string;
  readonly ingresos: number;
  readonly costosDirectos: number;
  readonly gastosGenerales: number;
  readonly utilidadBruta: number;
  /** Utilidad neta: ingresos menos costos directos y gastos generales. */
  readonly ganancia: number;
  readonly margenBruto: number | null;
  readonly margenNeto: number | null;
}

export interface ResultadoCliente {
  readonly idCliente: number;
  readonly cliente: string;
  readonly proyectosContratados: number;
  readonly proyectosConMovimientos: number;
  readonly ingresos: number;
  readonly costos: number;
  readonly margen: number;
  readonly porcentaje: number | null;
}

/** Margen de los proyectos de clientes recurrentes en los ultimos doce meses. */
export interface RentabilidadRecurrente {
  readonly desde: string;
  readonly hasta: string;
  readonly minimoProyectos: number;
  readonly clientesRecurrentes: number;
  readonly clientesConProyectos: number;
  readonly ingresos: number;
  readonly costos: number;
  readonly margen: number;
  readonly porcentaje: number | null;
  readonly margenTotalProyectos: number;
  /** Parte del margen anual de proyectos que aportan; nula si ese margen no es positivo. */
  readonly participacion: number | null;
  readonly clientes: readonly ResultadoCliente[];
}

export interface TasaUtilizacion {
  readonly recursos: number;
  readonly diasDisponibles: number;
  readonly diasOcupados: number;
  readonly porcentaje: number | null;
}

export interface UtilizacionCategoria {
  readonly categoria: string;
  readonly tasa: TasaUtilizacion;
}

export interface UtilizacionPersonal {
  readonly idPersonal: number;
  readonly nombre: string;
  readonly tasa: TasaUtilizacion;
  readonly eventos: number;
  readonly horas: number;
}

export interface UtilizacionRecursos {
  readonly equipos: TasaUtilizacion;
  readonly porCategoria: readonly UtilizacionCategoria[];
  readonly personal: TasaUtilizacion;
  readonly porTecnico: readonly UtilizacionPersonal[];
}

export interface ResumenAlertas {
  readonly criticas: number;
  readonly advertencias: number;
  readonly informativas: number;
}

export interface KpisGerenciales {
  readonly actual: IndicadoresPeriodo;
  readonly anterior: IndicadoresPeriodo;
  readonly rentabilidadRecurrente: RentabilidadRecurrente;
  readonly utilizacion: UtilizacionRecursos;
  readonly alertas: ResumenAlertas;
}

export type SentidoCambio = 'sube' | 'baja' | 'igual';

/** Cambio de un margen expresado en puntos porcentuales. */
export interface CambioPuntos {
  readonly puntos: number;
  readonly sentido: SentidoCambio;
}

const DECIMALES_PUNTOS = 1;

/** Diferencia en puntos porcentuales; nula si alguno de los margenes no existe. */
export function cambioEnPuntos(actual: number | null, anterior: number | null): CambioPuntos | null {
  if (actual === null || anterior === null) {
    return null;
  }
  const puntos = Number((actual - anterior).toFixed(DECIMALES_PUNTOS));
  return { puntos: Math.abs(puntos), sentido: puntos > 0 ? 'sube' : puntos < 0 ? 'baja' : 'igual' };
}

/** Utilizacion que se considera baja (el gerente la revisa). */
export const UTILIZACION_BAJA = 20;

/** Ancho de la barra de utilizacion (0 a 100); sin dias disponibles no hay barra. */
export function anchoBarra(tasa: TasaUtilizacion): number {
  return Math.min(100, Math.max(0, tasa.porcentaje ?? 0));
}
