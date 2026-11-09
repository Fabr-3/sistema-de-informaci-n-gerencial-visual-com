import { PlanEvento, ResumenLogisticoEvento, estadoPlanificacion, periodosSugeridos } from './planificacion';

const EVENTO = {
  id: 7,
  nombre: 'Cena de gala',
  fechaInicio: '2026-12-05T19:00:00',
  fechaFin: '2026-12-05T23:30:00',
  estado: 'PROGRAMADO',
  idProyecto: 3,
  proyecto: 'Cena de gala de fin de año',
  cliente: 'Banco Andino de Desarrollo S.A.',
  lugar: 'Hotel Casa Grande',
  direccion: 'Av. Ballivián 958',
  ciudad: 'La Paz',
} as const;

describe('planificacion logistica', () => {
  const resumen = (jornadas: number, personal: number, cruces: number): ResumenLogisticoEvento => ({
    evento: EVENTO,
    jornadas,
    personal,
    cruces,
  });

  it('resume el avance y prioriza los cruces de personal', () => {
    expect(estadoPlanificacion(resumen(0, 0, 0))).toEqual({ etiqueta: 'Sin planificar', tipo: 'neutro' });
    expect(estadoPlanificacion(resumen(2, 0, 0)).etiqueta).toBe('Sin personal');
    expect(estadoPlanificacion(resumen(2, 4, 0)).tipo).toBe('activo');
    expect(estadoPlanificacion(resumen(2, 4, 1))).toEqual({ etiqueta: 'Personal con cruces', tipo: 'alerta' });
  });

  it('sugiere cada jornada, el evento y todo el plan como horarios de asignacion', () => {
    const plan: PlanEvento = {
      evento: EVENTO,
      editable: true,
      detallesTransporte: null,
      ventanaInicio: '2026-12-04T08:00:00',
      ventanaFin: '2026-12-06T04:00:00',
      jornadas: [
        { id: 1, tipo: 'MONTAJE', inicio: '2026-12-04T08:00:00', fin: '2026-12-04T18:00:00', descripcion: null },
        { id: 2, tipo: 'DESMONTAJE', inicio: '2026-12-06T00:00:00', fin: '2026-12-06T04:00:00', descripcion: null },
      ],
      asignaciones: [],
      avisos: [],
    };

    expect(periodosSugeridos(plan).map((periodo) => periodo.etiqueta)).toEqual([
      'Montaje 04/12',
      'Desmontaje 06/12',
      'Evento',
      'Todo el plan',
    ]);
    expect(periodosSugeridos({ ...plan, jornadas: [] })).toEqual([
      { etiqueta: 'Evento', inicio: EVENTO.fechaInicio, fin: EVENTO.fechaFin },
    ]);
  });
});
