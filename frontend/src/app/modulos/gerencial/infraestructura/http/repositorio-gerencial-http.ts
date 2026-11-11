import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { parametrosConsulta } from '../../../../compartido/utilidades/parametros-http';
import { URL_API } from '../../../../nucleo/configuracion/api';
import { AlertaActivo } from '../../dominio/modelos/alertas';
import { KpisGerenciales } from '../../dominio/modelos/kpis';
import { TipoReporte } from '../../dominio/modelos/reportes';
import { RepositorioGerencial } from '../../dominio/repositorios/repositorio-gerencial';

@Injectable()
export class RepositorioGerencialHttp extends RepositorioGerencial {
  private readonly http = inject(HttpClient);
  private readonly url = inject(URL_API) + '/gerencial';

  kpis(desde?: string, hasta?: string): Observable<KpisGerenciales> {
    return this.http.get<KpisGerenciales>(`${this.url}/kpis`, { params: parametrosConsulta({ desde, hasta }) });
  }

  alertasActivos(): Observable<AlertaActivo[]> {
    return this.http.get<AlertaActivo[]>(`${this.url}/alertas-activos`);
  }

  reporte(tipo: TipoReporte, desde: string, hasta: string): Observable<Blob> {
    return this.http.get(`${this.url}/reportes/${tipo}`, {
      params: parametrosConsulta({ desde, hasta }),
      responseType: 'blob',
    });
  }
}
