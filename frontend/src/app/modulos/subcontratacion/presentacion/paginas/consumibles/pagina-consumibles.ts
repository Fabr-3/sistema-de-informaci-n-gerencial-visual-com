import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { catchError, debounceTime, distinctUntilChanged, filter, of, switchMap, tap } from 'rxjs';

import {
  Pagina,
  SolicitudPagina,
  TAMANOS_PAGINA,
  TAMANO_PAGINA_POR_DEFECTO,
} from '../../../../../compartido/modelos/pagina';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { ServicioSesion } from '../../../../accesos/aplicacion/servicio-sesion';
import { Permisos } from '../../../../accesos/dominio/modelos/permisos';
import { GestionConsumibles } from '../../../aplicacion/gestion-subcontratacion';
import { Consumible, FiltroConsumibles } from '../../../dominio/modelos/consumible';
import { DatosDialogoStock, DialogoConsumible, DialogoMovimientosConsumible, DialogoStock } from './dialogos-consumibles';

const RETARDO_BUSQUEDA_MS = 300;
const COLUMNAS = ['nombre', 'stock', 'minimo', 'costo', 'estado', 'acciones'];

interface Criterios {
  readonly filtro: FiltroConsumibles;
  readonly solicitud: SolicitudPagina;
}

/** Catalogo de consumibles con su stock; advierte los que estan en el minimo o por debajo. */
@Component({
  selector: 'app-pagina-consumibles',
  imports: [
    CurrencyPipe,
    DecimalPipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSlideToggleModule,
    MatTableModule,
  ],
  templateUrl: './pagina-consumibles.html',
  styleUrl: '../subcontratacion.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaConsumibles {
  private readonly gestion = inject(GestionConsumibles);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly dialogos = inject(MatDialog);
  private readonly sesion = inject(ServicioSesion);

  protected readonly columnas = COLUMNAS;
  protected readonly tamanosPagina = TAMANOS_PAGINA;
  protected readonly puedeGestionar = this.sesion.tienePermiso(Permisos.PROVEEDORES_GESTIONAR);
  protected readonly puedeVerMovimientos = this.sesion.tieneAlgunPermiso(
    Permisos.PROVEEDORES_VER,
    Permisos.PROVEEDORES_GESTIONAR,
  );
  protected readonly busqueda = new FormControl('', { nonNullable: true });
  protected readonly soloStockBajo = new FormControl(false, { nonNullable: true });
  protected readonly resultado = signal<Pagina<Consumible> | null>(null);
  protected readonly cargando = signal(false);
  protected readonly criterios = signal<Criterios>({
    filtro: {},
    solicitud: { pagina: 0, tamano: TAMANO_PAGINA_POR_DEFECTO },
  });

  constructor() {
    this.busqueda.valueChanges
      .pipe(debounceTime(RETARDO_BUSQUEDA_MS), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((texto) => this.filtrar({ texto }));
    this.soloStockBajo.valueChanges.pipe(takeUntilDestroyed()).subscribe((soloStockBajo) => this.filtrar({ soloStockBajo }));

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

  protected editar(consumible?: Consumible): void {
    this.dialogos
      .open<DialogoConsumible, Consumible | undefined, Consumible>(DialogoConsumible, { data: consumible, width: '560px' })
      .afterClosed()
      .pipe(filter(Boolean))
      .subscribe(() => this.recargar(consumible ? 'Consumible actualizado' : 'Consumible registrado'));
  }

  protected moverStock(consumible: Consumible, modo: DatosDialogoStock['modo']): void {
    this.dialogos
      .open<DialogoStock, DatosDialogoStock, Consumible>(DialogoStock, { data: { consumible, modo }, width: '520px' })
      .afterClosed()
      .pipe(filter(Boolean))
      .subscribe(() => this.recargar(modo === 'INGRESO' ? 'Ingreso registrado' : 'Ajuste registrado'));
  }

  protected verMovimientos(consumible: Consumible): void {
    this.dialogos.open(DialogoMovimientosConsumible, { data: consumible, width: '760px' });
  }

  private filtrar(cambio: Partial<FiltroConsumibles>): void {
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
