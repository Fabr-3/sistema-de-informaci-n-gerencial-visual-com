import { Alerta } from '../../gerencial/dominio/modelos/alertas';
import { AsignacionPropia } from '../../logistica/dominio/modelos/personal';
import { asignacionesProximas, avisoAlertas, avisoAsignaciones } from './avisos';

const alerta = (severidad: Alerta['severidad']): Alerta =>
  ({ id: 1, severidad, tipo: 'SIN_USO', estado: 'PENDIENTE' }) as Alerta;

const asignacion = (inicio: string, fin: string, cruces = 0): AsignacionPropia =>
  ({ id: 1, inicio, fin, cruces: Array.from({ length: cruces }, () => ({})) }) as unknown as AsignacionPropia;

describe('avisos al ingresar', () => {
  const ahora = new Date('2026-11-20T08:00:00');

  it('resume las alertas pendientes destacando las criticas', () => {
    expect(avisoAlertas([])).toBeNull();
    expect(avisoAlertas([alerta('ADVERTENCIA')])).toBe('1 alerta pendiente');
    expect(avisoAlertas([alerta('CRITICA'), alerta('INFORMATIVA'), alerta('CRITICA')])).toBe(
      '3 alertas pendientes (2 críticas)',
    );
  });

  it('avisa las asignaciones de las proximas 48 horas y los cruces', () => {
    const manana = asignacion('2026-11-21T09:00:00', '2026-11-21T18:00:00', 1);
    const lejana = asignacion('2026-11-30T09:00:00', '2026-11-30T18:00:00');
    const terminada = asignacion('2026-11-19T09:00:00', '2026-11-19T18:00:00');

    expect(asignacionesProximas([manana, lejana, terminada], ahora)).toEqual([manana]);
    expect(avisoAsignaciones([manana, lejana], ahora)).toBe(
      'Tiene 1 asignación en las próximas 48 h y 1 asignación con cruce de horario',
    );
    expect(avisoAsignaciones([lejana], ahora)).toBeNull();
  });
});
