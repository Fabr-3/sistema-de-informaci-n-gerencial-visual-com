import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { parametrosConsulta } from '../../../../compartido/utilidades/parametros-http';
import { URL_API } from '../../../../nucleo/configuracion/api';
import { FiltroAuditoria, RegistroAuditoria } from '../../dominio/modelos/auditoria';
import { RepositorioAuditoria } from '../../dominio/repositorios/repositorio-auditoria';

@Injectable()
export class RepositorioAuditoriaHttp extends RepositorioAuditoria {
  private readonly http = inject(HttpClient);
  private readonly url = inject(URL_API) + '/auditoria';

  consultar(filtro: FiltroAuditoria, solicitud: SolicitudPagina): Observable<Pagina<RegistroAuditoria>> {
    const params = parametrosConsulta({ ...filtro, ...solicitud });
    return this.http.get<Pagina<RegistroAuditoria>>(this.url, { params });
  }
}
