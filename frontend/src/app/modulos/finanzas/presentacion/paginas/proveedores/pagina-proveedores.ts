import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, debounceTime, distinctUntilChanged, filter, of, switchMap, tap } from 'rxjs';

import {
  DatosConfirmacion,
  DialogoConfirmacion,
} from '../../../../../compartido/componentes/dialogo-confirmacion/dialogo-confirmacion';
import {
  Pagina,
  SolicitudPagina,
  TAMANOS_PAGINA,
  TAMANO_PAGINA_POR_DEFECTO,
} from '../../../../../compartido/modelos/pagina';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { ServicioSesion } from '../../../../accesos/aplicacion/servicio-sesion';
import { Permisos } from '../../../../accesos/dominio/modelos/permisos';
import { GestionProveedores } from '../../../aplicacion/gestion-proveedores';
import { FiltroProveedores, Proveedor } from '../../../dominio/modelos/proveedor';
import { DialogoProveedor } from './dialogo-proveedor';

const RETARDO_BUSQUEDA_MS = 300;
const COLUMNAS = ['razonSocial', 'nit', 'contacto', 'servicios', 'acciones'];

interface CriteriosProveedores {
  readonly filtro: FiltroProveedores;
  readonly solicitud: SolicitudPagina;
}

/** Proveedores de compras, alquileres y servicios externos. */
@Component({
  selector: 'app-pagina-proveedores',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './pagina-proveedores.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaProveedores {
  private readonly gestion = inject(GestionProveedores);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly dialogos = inject(MatDialog);

  protected readonly columnas = COLUMNAS;
  protected readonly tamanosPagina = TAMANOS_PAGINA;
  protected readonly puedeGestionar = inject(ServicioSesion).tieneAlgunPermiso(
    Permisos.FINANZAS_GESTIONAR,
    Permisos.PROVEEDORES_GESTIONAR,
  );
  protected readonly busqueda = new FormControl('', { nonNullable: true });
  protected readonly resultado = signal<Pagina<Proveedor> | null>(null);
  protected readonly cargando = signal(false);
  protected readonly criterios = signal<CriteriosProveedores>({
    filtro: {},
    solicitud: { pagina: 0, tamano: TAMANO_PAGINA_POR_DEFECTO },
  });

  constructor() {
    this.busqueda.valueChanges
      .pipe(debounceTime(RETARDO_BUSQUEDA_MS), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((texto) =>
        this.criterios.update(({ solicitud }) => ({ filtro: { texto }, solicitud: { ...solicitud, pagina: 0 } })),
      );

    toObservable(this.criterios)
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(({ filtro, solicitud }) =>
          this.gestion.listar(filtro, solicitud).pipe(
            catchError((error: unknown) => {
              this.notificaciones.error(error);
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        this.resultado.set(resultado);
        this.cargando.set(false);
      });
  }

  protected cambiarPagina(evento: PageEvent): void {
    this.criterios.update(({ filtro }) => ({ filtro, solicitud: { pagina: evento.pageIndex, tamano: evento.pageSize } }));
  }

  protected editar(proveedor?: Proveedor): void {
    this.dialogos
      .open<DialogoProveedor, Proveedor | undefined, Proveedor>(DialogoProveedor, { data: proveedor, width: '600px' })
      .afterClosed()
      .pipe(filter(Boolean))
      .subscribe(() => this.recargar(proveedor ? 'Proveedor actualizado' : 'Proveedor registrado'));
  }

  protected eliminar(proveedor: Proveedor): void {
    this.dialogos
      .open<DialogoConfirmacion, DatosConfirmacion, boolean>(DialogoConfirmacion, {
        data: {
          titulo: 'Eliminar proveedor',
          mensaje: `Se eliminará ${proveedor.razonSocial}. Un proveedor con movimientos no se puede eliminar.`,
          textoConfirmar: 'Eliminar',
          destructiva: true,
        },
        width: '460px',
      })
      .afterClosed()
      .pipe(
        filter((confirmado) => confirmado === true),
        switchMap(() => this.gestion.eliminar(proveedor.id)),
      )
      .subscribe({
        next: () => this.recargar('Proveedor eliminado'),
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  private recargar(mensaje: string): void {
    this.notificaciones.exito(mensaje);
    this.criterios.update((actuales) => ({ ...actuales }));
  }
}
