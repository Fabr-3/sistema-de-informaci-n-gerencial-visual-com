import { TipoEstadoVisual } from '../../../../compartido/modelos/estado-visual';

export const SEVERIDADES = {
  CRITICA: 'Crítica',
  ADVERTENCIA: 'Advertencia',
  INFORMATIVA: 'Informativa',
} as const;

export type Severidad = keyof typeof SEVERIDADES;

export const TIPOS_ALERTA = {
  RESERVA_EN_RIESGO: 'Reserva en riesgo',
  MANTENIMIENTO_VENCIDO: 'Mantenimiento vencido',
  MANTENIMIENTO_PROXIMO: 'Mantenimiento próximo',
  MANTENIMIENTO_PROLONGADO: 'Mantenimiento prolongado',
  MODULOS_DEFECTUOSOS: 'Módulos LED defectuosos',
  FALLAS_RECURRENTES: 'Fallas recurrentes',
  SIN_USO: 'Sin uso',
  STOCK_BAJO: 'Stock bajo de consumible',
} as const;

export type TipoAlerta = keyof typeof TIPOS_ALERTA;

export const ESTADOS_ALERTA = {
  PENDIENTE: 'Pendientes',
  ATENDIDA: 'Atendidas',
  RESUELTA: 'Resueltas',
} as const;

export type EstadoAlerta = keyof typeof ESTADOS_ALERTA;

/**
 * Alerta registrada: nace pendiente, el gerente la marca como atendida y pasa
 * a resuelta cuando la situacion desaparece. Las de stock no tienen activo.
 */
export interface Alerta {
  readonly id: number;
  readonly tipo: TipoAlerta;
  readonly severidad: Severidad;
  readonly idActivo: number | null;
  readonly idEvento: number | null;
  readonly clave: string;
  readonly codigo: string | null;
  readonly modelo: string | null;
  readonly categoria: string | null;
  readonly detalle: string;
  readonly fechaGeneracion: string;
  readonly estado: EstadoAlerta;
  readonly nombreAtendio: string | null;
  readonly fechaAtencion: string | null;
  readonly comentarioAtencion: string | null;
  readonly fechaResolucion: string | null;
}

const VISUAL_SEVERIDAD: Readonly<Record<Severidad, TipoEstadoVisual>> = {
  CRITICA: 'peligro',
  ADVERTENCIA: 'alerta',
  INFORMATIVA: 'neutro',
};

const ICONO_SEVERIDAD: Readonly<Record<Severidad, string>> = {
  CRITICA: 'error',
  ADVERTENCIA: 'warning',
  INFORMATIVA: 'info',
};

export function tipoVisualSeveridad(severidad: Severidad): TipoEstadoVisual {
  return VISUAL_SEVERIDAD[severidad];
}

export function iconoSeveridad(severidad: Severidad): string {
  return ICONO_SEVERIDAD[severidad];
}

/** Sin severidad o tipo: no filtra por ese criterio. */
export function filtrarAlertas(
  alertas: readonly Alerta[],
  severidad: Severidad | null,
  tipo: TipoAlerta | null,
): Alerta[] {
  return alertas.filter(
    (alerta) => (!severidad || alerta.severidad === severidad) && (!tipo || alerta.tipo === tipo),
  );
}

export function contarPorSeveridad(alertas: readonly Alerta[]): Record<Severidad, number> {
  const conteo: Record<Severidad, number> = { CRITICA: 0, ADVERTENCIA: 0, INFORMATIVA: 0 };
  alertas.forEach((alerta) => conteo[alerta.severidad]++);
  return conteo;
}
