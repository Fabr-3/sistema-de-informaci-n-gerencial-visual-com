import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { parametrosConsulta } from '../../../../compartido/utilidades/parametros-http';
import { URL_API } from '../../../../nucleo/configuracion/api';
import { DatosUsuario, FiltroUsuarios, NuevoUsuario, Usuario } from '../../dominio/modelos/usuario';
import { RepositorioUsuarios } from '../../dominio/repositorios/repositorio-usuarios';

@Injectable()
export class RepositorioUsuariosHttp extends RepositorioUsuarios {
  private readonly http = inject(HttpClient);
  private readonly url = inject(URL_API) + '/usuarios';

  listar(filtro: FiltroUsuarios, solicitud: SolicitudPagina): Observable<Pagina<Usuario>> {
    const params = parametrosConsulta({ ...filtro, ...solicitud });
    return this.http.get<Pagina<Usuario>>(this.url, { params });
  }

  crear(usuario: NuevoUsuario): Observable<Usuario> {
    return this.http.post<Usuario>(this.url, usuario);
  }

  actualizar(id: number, datos: DatosUsuario): Observable<Usuario> {
    return this.http.put<Usuario>(`${this.url}/${id}`, datos);
  }

  cambiarEstado(id: number, activo: boolean): Observable<Usuario> {
    return this.http.patch<Usuario>(`${this.url}/${id}/estado`, { activo });
  }

  restablecerContrasena(id: number, contrasenaTemporal: string): Observable<void> {
    return this.http.put<void>(`${this.url}/${id}/contrasena`, { contrasenaTemporal });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
