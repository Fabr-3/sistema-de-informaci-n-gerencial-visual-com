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
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Observable, catchError, debounceTime, distinctUntilChanged, filter, of, shareReplay, switchMap, tap } from 'rxjs';

import {
  DatosConfirmacion,
  DialogoConfirmacion,
} from '../../../../compartido/componentes/dialogo-confirmacion/dialogo-confirmacion';
import {
  Pagina,
  SolicitudPagina,
  TAMANOS_PAGINA,
  TAMANO_PAGINA_POR_DEFECTO,
} from '../../../../compartido/modelos/pagina';
import { ServicioNotificaciones } from '../../../../nucleo/notificaciones/servicio-notificaciones';
import { ServicioSesion } from '../../../accesos/aplicacion/servicio-sesion';
import { Permisos } from '../../../accesos/dominio/modelos/permisos';
import { GestionProveedores } from '../../aplicacion/gestion-proveedores';
import { CatalogosProveedores, FiltroProveedores, Proveedor } from '../../dominio/modelos/proveedor';
import { DatosDialogoProveedor, DialogoProveedor } from '../paginas/proveedores/dialogo-proveedor';

const RETARDO_BUSQUEDA_MS = 300;
const COLUMNAS = ['razonSocial', 'documento', 'contacto', 'servicios', 'estado', 'acciones'];

/** Valor del filtro de estado: todos, solo activos o solo anulados. */
type FiltroEstado = 'todos' | 'activos' | 'anulados';

const ACTIVO_POR_FILTRO: Record<FiltroEstado, boolean | undefined> = {
  todos: undefined,
  activos: true,
  anulados: false,
};

interface CriteriosProveedores {
  readonly filtro: FiltroProveedores;
  readonly solicitud: SolicitudPagina;
}

/**
 * Listado de proveedores con alta, edicion, anulacion o activacion y
 * eliminacion; se usa en Finanzas y en Subcontratacion.
 */
@Component({
  selector: 'app-lista-proveedores',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './lista-proveedores.html',
  styleUrl: './lista-proveedores.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ListaProveedores {
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
  protected readonly estado = new FormControl<FiltroEstado>('todos', { nonNullable: true });
  protected readonly resultado = signal<Pagina<Proveedor> | null>(null);
  protected readonly cargando = signal(false);
  protected readonly criterios = signal<CriteriosProveedores>({
    filtro: {},
    solicitud: { pagina: 0, tamano: TAMANO_PAGINA_POR_DEFECTO },
  });
  private catalogos$?: Observable<CatalogosProveedores>;

  constructor() {
    this.busqueda.valueChanges
      .pipe(debounceTime(RETARDO_BUSQUEDA_MS), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((texto) => this.filtrar({ texto }));
    this.estado.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((estado) => this.filtrar({ activo: ACTIVO_POR_FILTRO[estado] }));

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

  /** Los catalogos se piden una vez y se reutilizan en cada apertura del formulario. */
  protected editar(proveedor?: Proveedor): void {
    this.catalogos$ ??= this.gestion.catalogos().pipe(shareReplay(1));
    this.catalogos$
      .pipe(
        switchMap((catalogos) =>
          this.dialogos
            .open<DialogoProveedor, DatosDialogoProveedor, Proveedor>(DialogoProveedor, {
              data: { proveedor, catalogos },
              width: '680px',
            })
            .afterClosed(),
        ),
        filter(Boolean),
      )
      .subscribe({
        next: () => this.recargar(proveedor ? 'Proveedor actualizado' : 'Proveedor registrado'),
        error: (error: unknown) => {
          this.catalogos$ = undefined;
          this.notificaciones.error(error);
        },
      });
  }

  /** Anulado, el proveedor conserva su historial pero no se elige en movimientos ni subcontrataciones nuevos. */
  protected cambiarEstado(proveedor: Proveedor): void {
    const anular = proveedor.activo;
    this.dialogos
      .open<DialogoConfirmacion, DatosConfirmacion, boolean>(DialogoConfirmacion, {
        data: anular
          ? {
              titulo: 'Anular proveedor',
              mensaje: `${proveedor.razonSocial} ya no se podrá elegir en movimientos ni subcontrataciones nuevos. Su historial se conserva y puede activarlo después.`,
              textoConfirmar: 'Anular',
              destructiva: true,
            }
          : {
              titulo: 'Activar proveedor',
              mensaje: `${proveedor.razonSocial} se podrá elegir otra vez en movimientos y subcontrataciones.`,
              textoConfirmar: 'Activar',
            },
        width: '460px',
      })
      .afterClosed()
      .pipe(
        filter((confirmado) => confirmado === true),
        switchMap(() => this.gestion.cambiarEstado(proveedor.id, !anular)),
      )
      .subscribe({
        next: () => this.recargar(anular ? 'Proveedor anulado' : 'Proveedor activado'),
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  protected eliminar(proveedor: Proveedor): void {
    this.dialogos
      .open<DialogoConfirmacion, DatosConfirmacion, boolean>(DialogoConfirmacion, {
        data: {
          titulo: 'Eliminar proveedor',
          mensaje: `Se eliminará ${proveedor.razonSocial}. Un proveedor con movimientos no se puede eliminar; en ese caso, anúlelo.`,
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

  private filtrar(cambio: Partial<FiltroProveedores>): void {
    this.criterios.update(({ filtro, solicitud }) => ({
      filtro: { ...filtro, ...cambio },
      solicitud: { ...solicitud, pagina: 0 },
    }));
  }

  private recargar(mensaje: string): void {
    this.notificaciones.exito(mensaje);
    this.criterios.update((actuales) => ({ ...actuales }));
  }
}
