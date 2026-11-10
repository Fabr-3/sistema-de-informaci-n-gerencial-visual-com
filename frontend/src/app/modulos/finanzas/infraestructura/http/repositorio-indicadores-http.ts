import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { parametrosConsulta } from '../../../../compartido/utilidades/parametros-http';
import { URL_API } from '../../../../nucleo/configuracion/api';
import { IndicadoresFinancieros } from '../../dominio/modelos/indicadores';
import { RepositorioIndicadores } from '../../dominio/repositorios/repositorio-indicadores';

@Injectable()
export class RepositorioIndicadoresHttp extends RepositorioIndicadores {
  private readonly http = inject(HttpClient);
  private readonly url = inject(URL_API) + '/finanzas/indicadores';

  calcular(desde?: string, hasta?: string): Observable<IndicadoresFinancieros> {
    return this.http.get<IndicadoresFinancieros>(this.url, { params: parametrosConsulta({ desde, hasta }) });
  }
}
