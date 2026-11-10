import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { parametrosConsulta } from '../../../../compartido/utilidades/parametros-http';
import { URL_API } from '../../../../nucleo/configuracion/api';
import { DetalleResultadoProyecto, FiltroResultados, ResultadoProyecto } from '../../dominio/modelos/resultado';
import { RepositorioResultados } from '../../dominio/repositorios/repositorio-resultados';

@Injectable()
export class RepositorioResultadosHttp extends RepositorioResultados {
  private readonly http = inject(HttpClient);
  private readonly url = inject(URL_API) + '/finanzas/resultados/proyectos';

  listar(filtro: FiltroResultados, solicitud: SolicitudPagina): Observable<Pagina<ResultadoProyecto>> {
    return this.http.get<Pagina<ResultadoProyecto>>(this.url, { params: parametrosConsulta({ ...filtro, ...solicitud }) });
  }

  detalle(idProyecto: number): Observable<DetalleResultadoProyecto> {
    return this.http.get<DetalleResultadoProyecto>(`${this.url}/${idProyecto}`);
  }
}
