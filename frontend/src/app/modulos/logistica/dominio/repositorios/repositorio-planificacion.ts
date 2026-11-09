import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { MisAsignaciones, PersonalDisponible } from '../modelos/personal';
import {
  Asignacion,
  DatosAsignacion,
  DatosJornada,
  FiltroEventosLogistica,
  PlanEvento,
  ResumenLogisticoEvento,
} from '../modelos/planificacion';

/** Planificacion logistica por evento y asignaciones propias. */
export abstract class RepositorioPlanificacion {
  abstract listar(
    filtro: FiltroEventosLogistica,
    solicitud: SolicitudPagina,
  ): Observable<Pagina<ResumenLogisticoEvento>>;
  abstract obtener(idEvento: number): Observable<PlanEvento>;
  abstract guardarTransporte(idEvento: number, detallesTransporte: string): Observable<PlanEvento>;
  abstract guardarJornada(idEvento: number, idJornada: number | null, datos: DatosJornada): Observable<PlanEvento>;
  abstract quitarJornada(idEvento: number, idJornada: number): Observable<PlanEvento>;
  abstract guardarAsignacion(
    idEvento: number,
    idAsignacion: number | null,
    datos: DatosAsignacion,
  ): Observable<Asignacion>;
  abstract quitarAsignacion(idEvento: number, idAsignacion: number): Observable<void>;
  abstract disponibles(idEvento: number, inicio: string, fin: string): Observable<PersonalDisponible[]>;
  abstract misAsignaciones(): Observable<MisAsignaciones>;
}
