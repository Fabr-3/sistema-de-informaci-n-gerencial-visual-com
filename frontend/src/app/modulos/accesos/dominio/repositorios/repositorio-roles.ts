import { Observable } from 'rxjs';

import { Permiso } from '../modelos/permisos';
import { Rol } from '../modelos/rol';

export abstract class RepositorioRoles {
  abstract listarRoles(): Observable<Rol[]>;
  abstract listarPermisos(): Observable<Permiso[]>;
  abstract actualizarPermisos(idRol: number, idsPermisos: readonly number[]): Observable<Rol>;
}
