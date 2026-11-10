import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../compartido/modelos/pagina';
import { MisAsignaciones, PersonalDisponible } from '../dominio/modelos/personal';
import {
  Asignacion,
  DatosAsignacion,
  DatosJornada,
  FiltroEventosLogistica,
  PlanEvento,
  ResumenLogisticoEvento,
} from '../dominio/modelos/planificacion';
import { RepositorioPlanificacion } from '../dominio/repositorios/repositorio-planificacion';

/** Casos de uso de la planificacion logistica de los eventos. */
@Injectable({ providedIn: 'root' })
export class GestionPlanificacion {
  private readonly repositorio = inject(RepositorioPlanificacion);

  listar(filtro: FiltroEventosLogistica, solicitud: SolicitudPagina): Observable<Pagina<ResumenLogisticoEvento>> {
    const texto = filtro.texto?.trim();
    return this.repositorio.listar({ ...filtro, texto: texto ? texto : undefined }, solicitud);
  }

  obtener(idEvento: number): Observable<PlanEvento> {
    return this.repositorio.obtener(idEvento);
  }

  guardarTransporte(idEvento: number, detallesTransporte: string): Observable<PlanEvento> {
    return this.repositorio.guardarTransporte(idEvento, detallesTransporte.trim());
  }

  guardarJornada(idEvento: number, idJornada: number | null, datos: DatosJornada): Observable<PlanEvento> {
    return this.repositorio.guardarJornada(idEvento, idJornada, {
      ...datos,
      descripcion: datos.descripcion?.trim() || null,
    });
  }

  quitarJornada(idEvento: number, idJornada: number): Observable<PlanEvento> {
    return this.repositorio.quitarJornada(idEvento, idJornada);
  }

  guardarAsignacion(idEvento: number, idAsignacion: number | null, datos: DatosAsignacion): Observable<Asignacion> {
    return this.repositorio.guardarAsignacion(idEvento, idAsignacion, datos);
  }

  quitarAsignacion(idEvento: number, idAsignacion: number): Observable<void> {
    return this.repositorio.quitarAsignacion(idEvento, idAsignacion);
  }

  /** idAsignacion: la que se esta editando, para que no cuente como cruce consigo misma. */
  disponibles(
    idEvento: number,
    inicio: string,
    fin: string,
    idAsignacion: number | null,
  ): Observable<PersonalDisponible[]> {
    return this.repositorio.disponibles(idEvento, inicio, fin, idAsignacion);
  }

  misAsignaciones(): Observable<MisAsignaciones> {
    return this.repositorio.misAsignaciones();
  }
}
