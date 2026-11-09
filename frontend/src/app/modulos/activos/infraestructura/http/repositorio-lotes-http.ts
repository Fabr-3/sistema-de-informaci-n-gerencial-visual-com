import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { parametrosConsulta } from '../../../../compartido/utilidades/parametros-http';
import { URL_API } from '../../../../nucleo/configuracion/api';
import {
  Existencia,
  FiltroExistencias,
  MovimientoLote,
  NuevoMovimientoLote,
  TrasladoLote,
} from '../../dominio/modelos/lotes';
import { RepositorioLotes } from '../../dominio/repositorios/repositorio-lotes';

@Injectable()
export class RepositorioLotesHttp extends RepositorioLotes {
  private readonly http = inject(HttpClient);
  private readonly url = inject(URL_API) + '/activos/lotes';

  listar(filtro: FiltroExistencias): Observable<Existencia[]> {
    return this.http.get<Existencia[]>(this.url, { params: parametrosConsulta({ ...filtro }) });
  }

  listarMovimientos(idExistencia: number, solicitud: SolicitudPagina): Observable<Pagina<MovimientoLote>> {
    return this.http.get<Pagina<MovimientoLote>>(`${this.url}/${idExistencia}/movimientos`, {
      params: parametrosConsulta({ ...solicitud }),
    });
  }

  registrarMovimiento(movimiento: NuevoMovimientoLote): Observable<Existencia> {
    return this.http.post<Existencia>(`${this.url}/movimientos`, movimiento);
  }

  trasladar(traslado: TrasladoLote): Observable<Existencia[]> {
    return this.http.post<Existencia[]>(`${this.url}/traslados`, traslado);
  }
}
