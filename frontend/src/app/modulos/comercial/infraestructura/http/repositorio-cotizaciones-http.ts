import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { guardarRecurso } from '../../../../compartido/utilidades/guardar-recurso';
import { parametrosConsulta } from '../../../../compartido/utilidades/parametros-http';
import { URL_API } from '../../../../nucleo/configuracion/api';
import { Cotizacion, DatosCotizacion, FiltroCotizaciones } from '../../dominio/modelos/cotizacion';
import { RepositorioCotizaciones } from '../../dominio/repositorios/repositorio-cotizaciones';

@Injectable()
export class RepositorioCotizacionesHttp extends RepositorioCotizaciones {
  private readonly http = inject(HttpClient);
  private readonly url = inject(URL_API) + '/comercial/cotizaciones';

  listar(filtro: FiltroCotizaciones, solicitud: SolicitudPagina): Observable<Pagina<Cotizacion>> {
    return this.http.get<Pagina<Cotizacion>>(this.url, { params: parametrosConsulta({ ...filtro, ...solicitud }) });
  }

  obtener(id: number): Observable<Cotizacion> {
    return this.http.get<Cotizacion>(`${this.url}/${id}`);
  }

  guardar(id: number | null, datos: DatosCotizacion): Observable<Cotizacion> {
    return guardarRecurso<Cotizacion>(this.http, this.url, id, datos);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  aceptar(id: number): Observable<Cotizacion> {
    return this.http.post<Cotizacion>(`${this.url}/${id}/aceptacion`, null);
  }

  rechazar(id: number, motivo: string): Observable<Cotizacion> {
    return this.http.post<Cotizacion>(`${this.url}/${id}/rechazo`, { motivo });
  }

  descargarPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.url}/${id}/pdf`, { responseType: 'blob' });
  }
}
