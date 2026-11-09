import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { guardarRecurso } from '../../../../compartido/utilidades/guardar-recurso';
import { parametrosConsulta } from '../../../../compartido/utilidades/parametros-http';
import { URL_API } from '../../../../nucleo/configuracion/api';
import { MisAsignaciones, PersonalDisponible } from '../../dominio/modelos/personal';
import {
  Asignacion,
  DatosAsignacion,
  DatosJornada,
  FiltroEventosLogistica,
  PlanEvento,
  ResumenLogisticoEvento,
} from '../../dominio/modelos/planificacion';
import { RepositorioPlanificacion } from '../../dominio/repositorios/repositorio-planificacion';

@Injectable()
export class RepositorioPlanificacionHttp extends RepositorioPlanificacion {
  private readonly http = inject(HttpClient);
  private readonly url = inject(URL_API) + '/logistica/eventos';
  private readonly urlPropias = inject(URL_API) + '/logistica/mis-asignaciones';

  listar(filtro: FiltroEventosLogistica, solicitud: SolicitudPagina): Observable<Pagina<ResumenLogisticoEvento>> {
    return this.http.get<Pagina<ResumenLogisticoEvento>>(this.url, {
      params: parametrosConsulta({ ...filtro, ...solicitud }),
    });
  }

  obtener(idEvento: number): Observable<PlanEvento> {
    return this.http.get<PlanEvento>(`${this.url}/${idEvento}`);
  }

  guardarTransporte(idEvento: number, detallesTransporte: string): Observable<PlanEvento> {
    return this.http.put<PlanEvento>(`${this.url}/${idEvento}/transporte`, { detallesTransporte });
  }

  guardarJornada(idEvento: number, idJornada: number | null, datos: DatosJornada): Observable<PlanEvento> {
    return guardarRecurso<PlanEvento>(this.http, `${this.url}/${idEvento}/jornadas`, idJornada, datos);
  }

  quitarJornada(idEvento: number, idJornada: number): Observable<PlanEvento> {
    return this.http.delete<PlanEvento>(`${this.url}/${idEvento}/jornadas/${idJornada}`);
  }

  guardarAsignacion(idEvento: number, idAsignacion: number | null, datos: DatosAsignacion): Observable<Asignacion> {
    return guardarRecurso<Asignacion>(this.http, `${this.url}/${idEvento}/asignaciones`, idAsignacion, datos);
  }

  quitarAsignacion(idEvento: number, idAsignacion: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${idEvento}/asignaciones/${idAsignacion}`);
  }

  disponibles(idEvento: number, inicio: string, fin: string): Observable<PersonalDisponible[]> {
    return this.http.get<PersonalDisponible[]>(`${this.url}/${idEvento}/personal-disponible`, {
      params: new HttpParams().set('inicio', inicio).set('fin', fin),
    });
  }

  misAsignaciones(): Observable<MisAsignaciones> {
    return this.http.get<MisAsignaciones>(this.urlPropias);
  }
}
