import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { guardarRecurso } from '../../../../compartido/utilidades/guardar-recurso';
import { parametrosConsulta } from '../../../../compartido/utilidades/parametros-http';
import { URL_API } from '../../../../nucleo/configuracion/api';
import {
  Contrato,
  DatosContrato,
  DatosEvento,
  DatosProyecto,
  DetalleProyecto,
  EstadoEvento,
  EstadoProyecto,
  Evento,
  EventoCalendario,
  FiltroProyectos,
  Proyecto,
} from '../../dominio/modelos/proyecto';
import { RepositorioProyectos } from '../../dominio/repositorios/repositorio-proyectos';

/** Nombre del campo multipart que espera el backend. */
const CAMPO_ARCHIVO = 'archivo';

@Injectable()
export class RepositorioProyectosHttp extends RepositorioProyectos {
  private readonly http = inject(HttpClient);
  private readonly url = inject(URL_API) + '/comercial/proyectos';
  private readonly urlCronograma = inject(URL_API) + '/comercial/cronograma';

  listar(filtro: FiltroProyectos, solicitud: SolicitudPagina): Observable<Pagina<Proyecto>> {
    return this.http.get<Pagina<Proyecto>>(this.url, { params: parametrosConsulta({ ...filtro, ...solicitud }) });
  }

  obtener(id: number): Observable<DetalleProyecto> {
    return this.http.get<DetalleProyecto>(`${this.url}/${id}`);
  }

  actualizar(id: number, datos: DatosProyecto): Observable<Proyecto> {
    return this.http.put<Proyecto>(`${this.url}/${id}`, datos);
  }

  cambiarEstado(id: number, estado: EstadoProyecto): Observable<Proyecto> {
    return this.http.patch<Proyecto>(`${this.url}/${id}/estado`, { estado });
  }

  guardarEvento(idProyecto: number, idEvento: number | null, datos: DatosEvento): Observable<Evento> {
    return guardarRecurso<Evento>(this.http, `${this.url}/${idProyecto}/eventos`, idEvento, datos);
  }

  cambiarEstadoEvento(idProyecto: number, idEvento: number, estado: EstadoEvento): Observable<Evento> {
    return this.http.patch<Evento>(`${this.url}/${idProyecto}/eventos/${idEvento}/estado`, { estado });
  }

  guardarContrato(idProyecto: number, idContrato: number | null, datos: DatosContrato): Observable<Contrato> {
    return guardarRecurso<Contrato>(this.http, `${this.url}/${idProyecto}/contratos`, idContrato, datos);
  }

  descargarContrato(idProyecto: number, idContrato: number): Observable<Blob> {
    return this.http.get(`${this.url}/${idProyecto}/contratos/${idContrato}/pdf`, { responseType: 'blob' });
  }

  adjuntarFirmado(idProyecto: number, idContrato: number, archivo: File): Observable<Contrato> {
    const formulario = new FormData();
    formulario.append(CAMPO_ARCHIVO, archivo);
    return this.http.put<Contrato>(`${this.url}/${idProyecto}/contratos/${idContrato}/firmado`, formulario);
  }

  descargarFirmado(idProyecto: number, idContrato: number): Observable<Blob> {
    return this.http.get(`${this.url}/${idProyecto}/contratos/${idContrato}/firmado`, { responseType: 'blob' });
  }

  cronograma(desde: string, hasta: string): Observable<EventoCalendario[]> {
    return this.http.get<EventoCalendario[]>(this.urlCronograma, { params: parametrosConsulta({ desde, hasta }) });
  }
}
