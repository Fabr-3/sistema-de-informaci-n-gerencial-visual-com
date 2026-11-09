import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Observable, filter, forkJoin, switchMap } from 'rxjs';

import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { ServicioSesion } from '../../../../accesos/aplicacion/servicio-sesion';
import { Permisos } from '../../../../accesos/dominio/modelos/permisos';
import { SiPermiso } from '../../../../accesos/presentacion/directivas/si-permiso';
import { GestionComercial } from '../../../aplicacion/gestion-comercial';
import { Lugar, Servicio } from '../../../dominio/modelos/catalogos';
import { DialogoLugar, DialogoServicio } from './dialogos-catalogos';

/** Servicios ofrecidos (con precio referencial) y lugares de eventos. */
@Component({
  selector: 'app-pagina-catalogos-comerciales',
  imports: [CurrencyPipe, MatButtonModule, MatIconModule, MatTableModule, MatTabsModule, MatTooltipModule, SiPermiso],
  templateUrl: './pagina-catalogos-comerciales.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaCatalogosComerciales {
  private readonly gestion = inject(GestionComercial);
  private readonly sesion = inject(ServicioSesion);
  private readonly dialogos = inject(MatDialog);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly permisos = Permisos;
  protected readonly servicios = signal<Servicio[]>([]);
  protected readonly lugares = signal<Lugar[]>([]);
  private readonly version = signal(0);
  private readonly puedeGestionar = computed(() => this.sesion.tienePermiso(Permisos.COMERCIAL_GESTIONAR));
  protected readonly columnasServicios = computed(() => {
    const base = ['nombre', 'unidad', 'precio', 'estado'];
    return this.puedeGestionar() ? [...base, 'acciones'] : base;
  });
  protected readonly columnasLugares = computed(() => {
    const base = ['nombre', 'direccion', 'ciudad'];
    return this.puedeGestionar() ? [...base, 'acciones'] : base;
  });

  constructor() {
    toObservable(this.version)
      .pipe(
        switchMap(() => forkJoin({ servicios: this.gestion.listarServicios(), lugares: this.gestion.listarLugares() })),
        takeUntilDestroyed(),
      )
      .subscribe({
        next: ({ servicios, lugares }) => {
          this.servicios.set(servicios);
          this.lugares.set(lugares);
        },
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  protected editarServicio(servicio?: Servicio): void {
    this.alCerrar(this.dialogos.open(DialogoServicio, { data: servicio ?? null, width: '560px' }).afterClosed(),
      'Servicio guardado');
  }

  protected editarLugar(lugar?: Lugar): void {
    this.alCerrar(this.dialogos.open(DialogoLugar, { data: lugar ?? null, width: '520px' }).afterClosed(),
      'Lugar guardado');
  }

  private alCerrar(resultado: Observable<unknown>, mensaje: string): void {
    resultado.pipe(filter(Boolean)).subscribe(() => {
      this.notificaciones.exito(mensaje);
      this.version.update((valor) => valor + 1);
    });
  }
}
