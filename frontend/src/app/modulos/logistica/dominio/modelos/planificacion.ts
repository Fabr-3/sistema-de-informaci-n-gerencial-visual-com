import { TipoEstadoVisual } from '../../../../compartido/modelos/estado-visual';
import { ElementoCatalogo } from '../../../comercial/dominio/modelos/catalogos';
import { EstadoEvento } from '../../../comercial/dominio/modelos/proyecto';

export const TIPOS_JORNADA = {
  MONTAJE: 'Montaje',
  DESMONTAJE: 'Desmontaje',
} as const;

export type TipoJornada = keyof typeof TIPOS_JORNADA;

/** Evento comercial visto desde la logistica (incluye la direccion del lugar). */
export interface EventoLogistico {
  readonly id: number;
  readonly nombre: string;
  readonly fechaInicio: string;
  readonly fechaFin: string;
  readonly estado: EstadoEvento;
  readonly idProyecto: number;
  readonly proyecto: string;
  readonly cliente: string;
  readonly lugar: string;
  readonly direccion: string | null;
  readonly ciudad: string | null;
}

export interface ResumenLogisticoEvento {
  readonly evento: EventoLogistico;
  readonly jornadas: number;
  readonly personal: number;
  readonly cruces: number;
}

/** Otra asignacion de la misma persona que se superpone (solo se alerta). */
export interface Cruce {
  readonly idAsignacion: number;
  readonly idPersonal: number;
  readonly idEvento: number;
  readonly evento: string;
  readonly inicio: string;
  readonly fin: string;
}

export interface Jornada {
  readonly id: number;
  readonly tipo: TipoJornada;
  readonly inicio: string;
  readonly fin: string;
  readonly descripcion: string | null;
}

export interface Asignacion {
  readonly id: number;
  readonly personal: { readonly id: number; readonly nombreCompleto: string };
  readonly rol: ElementoCatalogo;
  readonly inicio: string;
  readonly fin: string;
  readonly cruces: readonly Cruce[];
}

/** Plan logistico de un evento; la ventana va del primer montaje al ultimo desmontaje. */
export interface PlanEvento {
  readonly evento: EventoLogistico;
  readonly editable: boolean;
  readonly detallesTransporte: string | null;
  readonly ventanaInicio: string;
  readonly ventanaFin: string;
  readonly jornadas: readonly Jornada[];
  readonly asignaciones: readonly Asignacion[];
  readonly avisos: readonly string[];
}

export interface DatosJornada {
  readonly tipo: TipoJornada;
  readonly inicio: string;
  readonly fin: string;
  readonly descripcion: string | null;
}

export interface DatosAsignacion {
  readonly idPersonal: number;
  readonly idRol: number;
  readonly inicio: string;
  readonly fin: string;
}

/** Fechas en formato ISO (AAAA-MM-DD), ambas incluidas. */
export interface FiltroEventosLogistica {
  readonly desde?: string;
  readonly hasta?: string;
  readonly texto?: string;
}

export interface EstadoPlanificacion {
  readonly etiqueta: string;
  readonly tipo: TipoEstadoVisual;
}

/** Resume el avance: los cruces de personal prevalecen como alerta. */
export function estadoPlanificacion(resumen: ResumenLogisticoEvento): EstadoPlanificacion {
  if (resumen.cruces > 0) {
    return { etiqueta: 'Personal con cruces', tipo: 'alerta' };
  }
  if (resumen.personal === 0) {
    return resumen.jornadas === 0
      ? { etiqueta: 'Sin planificar', tipo: 'neutro' }
      : { etiqueta: 'Sin personal', tipo: 'alerta' };
  }
  return { etiqueta: 'Personal asignado', tipo: 'activo' };
}

/** Periodo sugerido para asignar personal: una jornada, el evento o todo el plan. */
export interface PeriodoSugerido {
  readonly etiqueta: string;
  readonly inicio: string;
  readonly fin: string;
}

export function periodosSugeridos(plan: PlanEvento): PeriodoSugerido[] {
  const jornadas = plan.jornadas.map((jornada) => ({
    etiqueta: `${TIPOS_JORNADA[jornada.tipo]} ${jornada.inicio.substring(8, 10)}/${jornada.inicio.substring(5, 7)}`,
    inicio: jornada.inicio,
    fin: jornada.fin,
  }));
  const evento = { etiqueta: 'Evento', inicio: plan.evento.fechaInicio, fin: plan.evento.fechaFin };
  const todo = { etiqueta: 'Todo el plan', inicio: plan.ventanaInicio, fin: plan.ventanaFin };
  return plan.jornadas.length ? [...jornadas, evento, todo] : [evento];
}
