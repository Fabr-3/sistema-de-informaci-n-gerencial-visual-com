import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, debounceTime, distinctUntilChanged, filter, forkJoin, of, switchMap, tap } from 'rxjs';

import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { ServicioSesion } from '../../../../accesos/aplicacion/servicio-sesion';
import { Permisos } from '../../../../accesos/dominio/modelos/permisos';
import { SiPermiso } from '../../../../accesos/presentacion/directivas/si-permiso';
import { CatalogosActivos } from '../../../aplicacion/catalogos-activos';
import { InventarioLotes } from '../../../aplicacion/inventario-lotes';
import { Bodega, Categoria } from '../../../dominio/modelos/catalogos';
import { Existencia, FiltroExistencias } from '../../../dominio/modelos/lotes';
import { ModeloActivo } from '../../../dominio/modelos/modelo-activo';
import { DialogoHistorialLote } from './dialogo-historial-lote';
import { DatosDialogoMovimiento, DialogoMovimientoLote, ModoMovimiento } from './dialogo-movimiento-lote';

const RETARDO_BUSQUEDA_MS = 300;
const COLUMNAS_BASE = ['modelo', 'categoria', 'bodega', 'cantidad'];

/** Existencias de los activos controlados por lote (cables, accesorios) por bodega. */
@Component({
  selector: 'app-pagina-lotes',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
    SiPermiso,
  ],
  templateUrl: './pagina-lotes.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaLotes {
  private readonly inventario = inject(InventarioLotes);
  private readonly catalogos = inject(CatalogosActivos);
  private readonly sesion = inject(ServicioSesion);
  private readonly dialogos = inject(MatDialog);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly permisos = Permisos;
  protected readonly busqueda = new FormControl('', { nonNullable: true });
  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly bodegas = signal<Bodega[]>([]);
  private readonly modelosLote = signal<ModeloActivo[]>([]);
  protected readonly existencias = signal<Existencia[]>([]);
  protected readonly cargando = signal(false);
  protected readonly filtro = signal<FiltroExistencias>({});
  private readonly version = signal(0);

  protected readonly columnas = computed(() =>
    this.sesion.tienePermiso(Permisos.ACTIVOS_GESTIONAR) ? [...COLUMNAS_BASE, 'acciones'] : [...COLUMNAS_BASE, 'historial'],
  );
  protected readonly totalUnidades = computed(() =>
    this.existencias().reduce((total, existencia) => total + existencia.cantidad, 0),
  );

  constructor() {
    forkJoin({
      categorias: this.catalogos.listarCategorias(),
      bodegas: this.catalogos.listarBodegas(),
      modelos: this.catalogos.listarModelos('LOTE'),
    })
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: ({ categorias, bodegas, modelos }) => {
          this.categorias.set(categorias);
          this.bodegas.set(bodegas);
          this.modelosLote.set(modelos);
        },
        error: (error: unknown) => this.notificaciones.error(error),
      });

    this.busqueda.valueChanges
      .pipe(debounceTime(RETARDO_BUSQUEDA_MS), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((texto) => this.filtrar({ texto }));

    toObservable(computed(() => ({ filtro: this.filtro(), version: this.version() })))
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(({ filtro }) =>
          this.inventario.listar(filtro).pipe(
            catchError((error: unknown) => {
              this.notificaciones.error(error);
              return of([]);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((existencias) => {
        this.existencias.set(existencias);
        this.cargando.set(false);
      });
  }

  protected filtrar(cambios: Partial<FiltroExistencias>): void {
    this.filtro.update((actual) => ({ ...actual, ...cambios }));
  }

  protected registrar(modo: ModoMovimiento, existencia?: Existencia): void {
    this.dialogos
      .open<DialogoMovimientoLote, DatosDialogoMovimiento, boolean>(DialogoMovimientoLote, {
        data: {
          modo,
          modelos: this.modelosLote(),
          bodegas: this.bodegas(),
          idModelo: existencia?.idModelo,
          idBodega: existencia?.idBodega,
        },
        width: '600px',
      })
      .afterClosed()
      .pipe(filter(Boolean))
      .subscribe(() => {
        this.notificaciones.exito(modo === 'traslado' ? 'Traslado registrado' : 'Movimiento registrado');
        this.version.update((valor) => valor + 1);
      });
  }

  protected verHistorial(existencia: Existencia): void {
    this.dialogos.open(DialogoHistorialLote, { data: existencia, width: '760px' });
  }
}
