import { Alerta } from '../../gerencial/dominio/modelos/alertas';
import { AsignacionPropia } from '../../logistica/dominio/modelos/personal';

/** Ventana en la que una asignacion se considera inminente para avisarla al ingresar. */
export const HORAS_ASIGNACION_PROXIMA = 48;
const MILISEGUNDOS_POR_HORA = 3_600_000;

/** Texto del aviso de alertas pendientes; nulo si no hay ninguna. */
export function avisoAlertas(alertas: readonly Alerta[]): string | null {
  if (!alertas.length) {
    return null;
  }
  const criticas = alertas.filter((alerta) => alerta.severidad === 'CRITICA').length;
  const total = alertas.length === 1 ? '1 alerta pendiente' : `${alertas.length} alertas pendientes`;
  return criticas ? `${total} (${criticas === 1 ? '1 crítica' : `${criticas} críticas`})` : total;
}

/** Asignaciones que empiezan dentro de la ventana indicada (o ya en curso). */
export function asignacionesProximas(
  asignaciones: readonly AsignacionPropia[],
  ahora: Date,
  horas = HORAS_ASIGNACION_PROXIMA,
): AsignacionPropia[] {
  const limite = ahora.getTime() + horas * MILISEGUNDOS_POR_HORA;
  return asignaciones.filter(
    (asignacion) => new Date(asignacion.inicio).getTime() <= limite && new Date(asignacion.fin).getTime() > ahora.getTime(),
  );
}

/** Texto del aviso de asignaciones proximas y cruces; nulo si no hay nada que avisar. */
export function avisoAsignaciones(asignaciones: readonly AsignacionPropia[], ahora: Date): string | null {
  const proximas = asignacionesProximas(asignaciones, ahora).length;
  const conCruce = asignaciones.filter((asignacion) => asignacion.cruces.length).length;
  const partes = [
    proximas ? `${proximas === 1 ? '1 asignación' : `${proximas} asignaciones`} en las próximas ${HORAS_ASIGNACION_PROXIMA} h` : '',
    conCruce ? `${conCruce === 1 ? '1 asignación' : `${conCruce} asignaciones`} con cruce de horario` : '',
  ].filter(Boolean);
  return partes.length ? `Tiene ${partes.join(' y ')}` : null;
}
