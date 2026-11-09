import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { finalize, forkJoin } from 'rxjs';

import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionRoles, GrupoPermisos } from '../../../aplicacion/gestion-roles';
import { ServicioSesion } from '../../../aplicacion/servicio-sesion';
import { Permisos } from '../../../dominio/modelos/permisos';
import { Rol } from '../../../dominio/modelos/rol';

/**
 * Asignacion de permisos a los roles fijos. Cada rol se edita y guarda por
 * separado; el rol Administrador se muestra en solo lectura.
 */
@Component({
  selector: 'app-pagina-roles',
  imports: [MatButtonModule, MatCheckboxModule, MatExpansionModule, MatIconModule, MatProgressBarModule],
  templateUrl: './pagina-roles.html',
  styleUrl: './pagina-roles.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaRoles {
  private readonly gestion = inject(GestionRoles);
  private readonly sesion = inject(ServicioSesion);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly roles = signal<Rol[]>([]);
  protected readonly grupos = signal<GrupoPermisos[]>([]);
  protected readonly cargando = signal(true);
  protected readonly guardando = signal<number | null>(null);
  /** Permisos marcados en pantalla por rol (aun no guardados). */
  protected readonly seleccion = signal<ReadonlyMap<number, ReadonlySet<number>>>(new Map());
  protected readonly puedeGestionar = computed(() => this.sesion.tienePermiso(Permisos.ROLES_GESTIONAR));

  constructor() {
    forkJoin({ roles: this.gestion.listarRoles(), permisos: this.gestion.listarPermisos() })
      .pipe(
        finalize(() => this.cargando.set(false)),
        takeUntilDestroyed(),
      )
      .subscribe({
        next: ({ roles, permisos }) => {
          this.grupos.set(this.gestion.agruparPorModulo(permisos));
          this.establecerRoles(roles);
        },
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  protected esEditable(rol: Rol): boolean {
    return rol.modificable && this.puedeGestionar();
  }

  protected estaMarcado(rol: Rol, idPermiso: number): boolean {
    return this.seleccion().get(rol.id)?.has(idPermiso) ?? false;
  }

  protected alternar(rol: Rol, idPermiso: number, marcado: boolean): void {
    this.seleccion.update((actual) => {
      const permisosRol = new Set(actual.get(rol.id));
      if (marcado) {
        permisosRol.add(idPermiso);
      } else {
        permisosRol.delete(idPermiso);
      }
      return new Map(actual).set(rol.id, permisosRol);
    });
  }

  protected tieneCambios(rol: Rol): boolean {
    const marcados = this.seleccion().get(rol.id) ?? new Set<number>();
    return marcados.size !== rol.permisos.length || rol.permisos.some((permiso) => !marcados.has(permiso.id));
  }

  protected descartar(rol: Rol): void {
    this.seleccion.update((actual) => new Map(actual).set(rol.id, idsPermisos(rol)));
  }

  protected guardar(rol: Rol): void {
    this.guardando.set(rol.id);
    this.gestion
      .actualizarPermisos(rol.id, this.seleccion().get(rol.id) ?? new Set())
      .pipe(finalize(() => this.guardando.set(null)))
      .subscribe({
        next: (actualizado) => {
          this.establecerRoles(this.roles().map((existente) => (existente.id === actualizado.id ? actualizado : existente)));
          this.notificaciones.exito(`Permisos de ${actualizado.nombre} actualizados`);
        },
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  private establecerRoles(roles: Rol[]): void {
    this.roles.set(roles);
    this.seleccion.set(new Map(roles.map((rol) => [rol.id, idsPermisos(rol)])));
  }
}

function idsPermisos(rol: Rol): ReadonlySet<number> {
  return new Set(rol.permisos.map((permiso) => permiso.id));
}
