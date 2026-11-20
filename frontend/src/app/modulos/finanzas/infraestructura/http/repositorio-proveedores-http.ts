import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { guardarRecurso } from '../../../../compartido/utilidades/guardar-recurso';
import { parametrosConsulta } from '../../../../compartido/utilidades/parametros-http';
import { URL_API } from '../../../../nucleo/configuracion/api';
import {
  CatalogosProveedores,
  DatosProveedor,
  FiltroProveedores,
  Proveedor,
} from '../../dominio/modelos/proveedor';
import { RepositorioProveedores } from '../../dominio/repositorios/repositorio-proveedores';

@Injectable()
export class RepositorioProveedoresHttp extends RepositorioProveedores {
  private readonly http = inject(HttpClient);
  private readonly url = inject(URL_API) + '/finanzas/proveedores';

  listar(filtro: FiltroProveedores, solicitud: SolicitudPagina): Observable<Pagina<Proveedor>> {
    return this.http.get<Pagina<Proveedor>>(this.url, { params: parametrosConsulta({ ...filtro, ...solicitud }) });
  }

  catalogos(): Observable<CatalogosProveedores> {
    return this.http.get<CatalogosProveedores>(`${this.url}/catalogos`);
  }

  cambiarEstado(id: number, activo: boolean): Observable<Proveedor> {
    return this.http.patch<Proveedor>(`${this.url}/${id}/estado`, { activo });
  }

  guardar(id: number | null, datos: DatosProveedor): Observable<Proveedor> {
    return guardarRecurso<Proveedor>(this.http, this.url, id, datos);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
