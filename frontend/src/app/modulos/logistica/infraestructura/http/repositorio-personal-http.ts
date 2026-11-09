import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { guardarRecurso } from '../../../../compartido/utilidades/guardar-recurso';
import { parametrosConsulta } from '../../../../compartido/utilidades/parametros-http';
import { URL_API } from '../../../../nucleo/configuracion/api';
import { CatalogosLogistica, DatosPersonal, FiltroPersonal, PersonalTecnico } from '../../dominio/modelos/personal';
import { RepositorioPersonal } from '../../dominio/repositorios/repositorio-personal';

@Injectable()
export class RepositorioPersonalHttp extends RepositorioPersonal {
  private readonly http = inject(HttpClient);
  private readonly url = inject(URL_API) + '/logistica/personal';
  private readonly urlCatalogos = inject(URL_API) + '/logistica/catalogos';

  listar(filtro: FiltroPersonal, solicitud: SolicitudPagina): Observable<Pagina<PersonalTecnico>> {
    return this.http.get<Pagina<PersonalTecnico>>(this.url, { params: parametrosConsulta({ ...filtro, ...solicitud }) });
  }

  guardar(id: number | null, datos: DatosPersonal): Observable<PersonalTecnico> {
    return guardarRecurso<PersonalTecnico>(this.http, this.url, id, datos);
  }

  catalogos(): Observable<CatalogosLogistica> {
    return this.http.get<CatalogosLogistica>(this.urlCatalogos);
  }
}
