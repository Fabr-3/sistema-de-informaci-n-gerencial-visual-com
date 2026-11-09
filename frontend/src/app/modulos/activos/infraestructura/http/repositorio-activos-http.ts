import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { parametrosConsulta } from '../../../../compartido/utilidades/parametros-http';
import { URL_API } from '../../../../nucleo/configuracion/api';
import { Activo, DatosActivo, FiltroActivos, RegistroActivos } from '../../dominio/modelos/activo';
import { RepositorioActivos } from '../../dominio/repositorios/repositorio-activos';

@Injectable()
export class RepositorioActivosHttp extends RepositorioActivos {
  private readonly http = inject(HttpClient);
  private readonly url = inject(URL_API) + '/activos';

  listar(filtro: FiltroActivos, solicitud: SolicitudPagina): Observable<Pagina<Activo>> {
    return this.http.get<Pagina<Activo>>(this.url, { params: parametrosConsulta({ ...filtro, ...solicitud }) });
  }

  listarAlertasMantenimiento(): Observable<Activo[]> {
    return this.http.get<Activo[]>(`${this.url}/alertas-mantenimiento`);
  }

  obtener(id: number): Observable<Activo> {
    return this.http.get<Activo>(`${this.url}/${id}`);
  }

  registrar(registro: RegistroActivos): Observable<Activo[]> {
    return this.http.post<Activo[]>(this.url, registro);
  }

  actualizar(id: number, datos: DatosActivo): Observable<Activo> {
    return this.http.put<Activo>(`${this.url}/${id}`, datos);
  }

  darDeBaja(id: number, motivo: string): Observable<Activo> {
    return this.http.post<Activo>(`${this.url}/${id}/baja`, { motivo });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
