import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { NOMBRES_MODULOS, Permiso } from '../dominio/modelos/permisos';
import { Rol } from '../dominio/modelos/rol';
import { RepositorioRoles } from '../dominio/repositorios/repositorio-roles';

export interface GrupoPermisos {
  readonly modulo: string;
  readonly nombreModulo: string;
  readonly permisos: readonly Permiso[];
}

/** Casos de uso de roles y asignacion de permisos. */
@Injectable({ providedIn: 'root' })
export class GestionRoles {
  private readonly repositorio = inject(RepositorioRoles);

  listarRoles(): Observable<Rol[]> {
    return this.repositorio.listarRoles();
  }

  listarPermisos(): Observable<Permiso[]> {
    return this.repositorio.listarPermisos();
  }

  actualizarPermisos(idRol: number, idsPermisos: ReadonlySet<number>): Observable<Rol> {
    return this.repositorio.actualizarPermisos(idRol, [...idsPermisos]);
  }

  /** Agrupa los permisos por modulo conservando el orden recibido. */
  agruparPorModulo(permisos: readonly Permiso[]): GrupoPermisos[] {
    const grupos = new Map<string, Permiso[]>();
    for (const permiso of permisos) {
      const grupo = grupos.get(permiso.modulo) ?? [];
      grupo.push(permiso);
      grupos.set(permiso.modulo, grupo);
    }
    return [...grupos].map(([modulo, permisosModulo]) => ({
      modulo,
      nombreModulo: NOMBRES_MODULOS[modulo] ?? modulo,
      permisos: permisosModulo,
    }));
  }
}
