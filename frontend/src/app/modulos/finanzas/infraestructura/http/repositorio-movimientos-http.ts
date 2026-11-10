import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { parametrosConsulta } from '../../../../compartido/utilidades/parametros-http';
import { URL_API } from '../../../../nucleo/configuracion/api';
import {
  DatosMovimiento,
  FiltroMovimientos,
  ListadoMovimientos,
  Movimiento,
  TipoMovimiento,
} from '../../dominio/modelos/movimiento';
import { RepositorioMovimientos } from '../../dominio/repositorios/repositorio-movimientos';

@Injectable()
export class RepositorioMovimientosHttp extends RepositorioMovimientos {
  private readonly http = inject(HttpClient);
  private readonly urlBase = inject(URL_API) + '/finanzas';
  private readonly url = this.urlBase + '/movimientos';

  tipos(): Observable<TipoMovimiento[]> {
    return this.http.get<TipoMovimiento[]>(`${this.urlBase}/tipos-movimiento`);
  }

  listar(filtro: FiltroMovimientos, solicitud: SolicitudPagina): Observable<ListadoMovimientos> {
    return this.http.get<ListadoMovimientos>(this.url, { params: parametrosConsulta({ ...filtro, ...solicitud }) });
  }

  registrar(datos: DatosMovimiento): Observable<Movimiento> {
    return this.http.post<Movimiento>(this.url, datos);
  }

  anular(id: number, motivo: string): Observable<Movimiento> {
    return this.http.post<Movimiento>(`${this.url}/${id}/anulacion`, { motivo });
  }
}
