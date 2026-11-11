export type TipoReporte = 'RESUMEN_GERENCIAL' | 'EVENTOS' | 'ESTADO_ACTIVOS' | 'MANTENIMIENTOS' | 'PERSONAL';

export interface DescripcionReporte {
  readonly tipo: TipoReporte;
  readonly titulo: string;
  readonly descripcion: string;
  readonly icono: string;
  /** Prefijo del archivo descargado (igual al que usa el servidor). */
  readonly archivo: string;
  /** El estado de activos refleja la situacion actual y no usa periodo. */
  readonly usaPeriodo: boolean;
}

/** Reportes operativos que se descargan en PDF. */
export const REPORTES: readonly DescripcionReporte[] = [
  {
    tipo: 'RESUMEN_GERENCIAL',
    archivo: 'resumen-gerencial',
    titulo: 'Resumen gerencial',
    descripcion: 'KPIs financieros frente al periodo anterior, rentabilidad recurrente, utilización de recursos y alertas.',
    icono: 'summarize',
    usaPeriodo: true,
  },
  {
    tipo: 'EVENTOS',
    archivo: 'eventos',
    titulo: 'Eventos del periodo',
    descripcion: 'Eventos con su proyecto, cliente, lugar, estado, personal asignado y jornadas de montaje.',
    icono: 'event',
    usaPeriodo: true,
  },
  {
    tipo: 'ESTADO_ACTIVOS',
    archivo: 'estado-activos',
    titulo: 'Estado de activos e inventario',
    descripcion: 'Equipos por categoría y estado, existencias por lote y alertas vigentes. Refleja la situación actual.',
    icono: 'inventory',
    usaPeriodo: false,
  },
  {
    tipo: 'MANTENIMIENTOS',
    archivo: 'mantenimientos',
    titulo: 'Mantenimientos de equipos',
    descripcion: 'Mantenimientos en curso durante el periodo, con su condición final y costo total.',
    icono: 'build',
    usaPeriodo: true,
  },
  {
    tipo: 'PERSONAL',
    archivo: 'utilizacion-personal',
    titulo: 'Utilización del personal técnico',
    descripcion: 'Días asignados, eventos y horas de cada técnico activo en el periodo.',
    icono: 'engineering',
    usaPeriodo: true,
  },
];

/** Nombre del PDF: con el periodo o, si no lo usa, con la fecha de emision. */
export function nombreArchivoReporte(reporte: DescripcionReporte, desde: string, hasta: string, hoy: string): string {
  const fechas = reporte.usaPeriodo ? `${desde}_${hasta}` : hoy;
  return `${reporte.archivo}_${fechas}.pdf`;
}
