import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { guardarRecurso } from '../../../../compartido/utilidades/guardar-recurso';
import { parametrosConsulta } from '../../../../compartido/utilidades/parametros-http';
import { URL_API } from '../../../../nucleo/configuracion/api';
import { Cliente, DatosCliente, FiltroClientes } from '../../dominio/modelos/cliente';
import { RepositorioClientes } from '../../dominio/repositorios/repositorio-clientes';

@Injectable()
export class RepositorioClientesHttp extends RepositorioClientes {
  private readonly http = inject(HttpClient);
  private readonly url = inject(URL_API) + '/comercial/clientes';

  listar(filtro: FiltroClientes, solicitud: SolicitudPagina): Observable<Pagina<Cliente>> {
    return this.http.get<Pagina<Cliente>>(this.url, { params: parametrosConsulta({ ...filtro, ...solicitud }) });
  }

  obtener(id: number): Observable<Cliente> {
    return this.http.get<Cliente>(`${this.url}/${id}`);
  }

  guardar(id: number | null, datos: DatosCliente): Observable<Cliente> {
    return guardarRecurso<Cliente>(this.http, this.url, id, datos);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
