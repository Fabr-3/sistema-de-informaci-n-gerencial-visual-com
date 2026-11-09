import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { URL_API } from '../../../../nucleo/configuracion/api';
import { Permiso } from '../../dominio/modelos/permisos';
import { Rol } from '../../dominio/modelos/rol';
import { RepositorioRoles } from '../../dominio/repositorios/repositorio-roles';

@Injectable()
export class RepositorioRolesHttp extends RepositorioRoles {
  private readonly http = inject(HttpClient);
  private readonly url = inject(URL_API);

  listarRoles(): Observable<Rol[]> {
    return this.http.get<Rol[]>(`${this.url}/roles`);
  }

  listarPermisos(): Observable<Permiso[]> {
    return this.http.get<Permiso[]>(`${this.url}/permisos`);
  }

  actualizarPermisos(idRol: number, idsPermisos: readonly number[]): Observable<Rol> {
    return this.http.put<Rol>(`${this.url}/roles/${idRol}/permisos`, { idsPermisos });
  }
}
