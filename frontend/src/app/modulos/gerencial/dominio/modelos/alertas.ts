import { TipoEstadoVisual } from '../../../../compartido/modelos/estado-visual';

export const SEVERIDADES = {
  CRITICA: 'Crítica',
  ADVERTENCIA: 'Advertencia',
  INFORMATIVA: 'Informativa',
} as const;

export type Severidad = keyof typeof SEVERIDADES;

export const TIPOS_ALERTA = {
  MANTENIMIENTO_VENCIDO: 'Mantenimiento vencido',
  MANTENIMIENTO_PROXIMO: 'Mantenimiento próximo',
  MANTENIMIENTO_PROLONGADO: 'Mantenimiento prolongado',
  MODULOS_DEFECTUOSOS: 'Módulos LED defectuosos',
  FALLAS_RECURRENTES: 'Fallas recurrentes',
  SIN_USO: 'Sin uso',
} as const;

export type TipoAlertaActivo = keyof typeof TIPOS_ALERTA;

export interface AlertaActivo {
  readonly tipo: TipoAlertaActivo;
  readonly severidad: Severidad;
  readonly idActivo: number;
  readonly codigo: string;
  readonly modelo: string;
  readonly categoria: string;
  readonly detalle: string;
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
  alertas: readonly AlertaActivo[],
  severidad: Severidad | null,
  tipo: TipoAlertaActivo | null,
): AlertaActivo[] {
  return alertas.filter(
    (alerta) => (!severidad || alerta.severidad === severidad) && (!tipo || alerta.tipo === tipo),
  );
}

export function contarPorSeveridad(alertas: readonly AlertaActivo[]): Record<Severidad, number> {
  const conteo: Record<Severidad, number> = { CRITICA: 0, ADVERTENCIA: 0, INFORMATIVA: 0 };
  alertas.forEach((alerta) => conteo[alerta.severidad]++);
  return conteo;
}
