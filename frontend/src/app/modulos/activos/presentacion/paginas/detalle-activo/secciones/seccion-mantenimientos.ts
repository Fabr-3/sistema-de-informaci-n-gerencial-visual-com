import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { catchError, combineLatest, filter, of, switchMap } from 'rxjs';

import { EtiquetaPipe } from '../../../../../../compartido/pipes/etiqueta.pipe';
import { Pagina, TAMANO_PAGINA_POR_DEFECTO } from '../../../../../../compartido/modelos/pagina';
import { ServicioNotificaciones } from '../../../../../../nucleo/notificaciones/servicio-notificaciones';
import { ServicioSesion } from '../../../../../accesos/aplicacion/servicio-sesion';
import { Permisos } from '../../../../../accesos/dominio/modelos/permisos';
import { HistorialActivos } from '../../../../aplicacion/historial-activos';
import { Activo, reglasActivo } from '../../../../dominio/modelos/activo';
import { CONDICIONES_FINALES, Mantenimiento } from '../../../../dominio/modelos/historial';
import { DialogoFinalizarMantenimiento } from '../dialogos/dialogo-finalizar-mantenimiento';
import { DialogoIniciarMantenimiento } from '../dialogos/dialogo-iniciar-mantenimiento';

/** Historial de mantenimientos del activo: inicio y finalizacion. */
@Component({
  selector: 'app-seccion-mantenimientos',
  imports: [CurrencyPipe, EtiquetaPipe, DatePipe, MatButtonModule, MatIconModule, MatPaginatorModule, MatTableModule],
  templateUrl: './seccion-mantenimientos.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SeccionMantenimientos {
  readonly activo = input.required<Activo>();
  readonly cambio = output<void>();

  private readonly historial = inject(HistorialActivos);
  private readonly sesion = inject(ServicioSesion);
  private readonly dialogos = inject(MatDialog);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly condiciones = CONDICIONES_FINALES;
  protected readonly columnas = ['tipo', 'inicio', 'fin', 'resultado', 'costo', 'detalle', 'acciones'];
  protected readonly pagina = signal(0);
  protected readonly tamano = TAMANO_PAGINA_POR_DEFECTO;
  protected readonly mantenimientos = signal<Pagina<Mantenimiento> | null>(null);
  protected readonly puedeRegistrar = computed(() => this.sesion.tienePermiso(Permisos.MANTENIMIENTOS_REGISTRAR));
  protected readonly puedeIniciar = computed(
    () => this.puedeRegistrar() && reglasActivo.puedeIniciarMantenimiento(this.activo()),
  );

  constructor() {
    combineLatest([toObservable(this.activo), toObservable(this.pagina)])
      .pipe(
        switchMap(([activo, pagina]) =>
          this.historial.listarMantenimientos(activo.id, { pagina, tamano: this.tamano }).pipe(
            catchError((error: unknown) => {
              this.notificaciones.error(error);
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((mantenimientos) => this.mantenimientos.set(mantenimientos));
  }

  protected cambiarPagina(evento: PageEvent): void {
    this.pagina.set(evento.pageIndex);
  }

  protected iniciar(): void {
    this.dialogos
      .open(DialogoIniciarMantenimiento, { data: this.activo(), width: '560px' })
      .afterClosed()
      .pipe(filter(Boolean))
      .subscribe(() => this.notificarCambio('Mantenimiento iniciado'));
  }

  protected finalizar(mantenimiento: Mantenimiento): void {
    this.dialogos
      .open(DialogoFinalizarMantenimiento, { data: mantenimiento, width: '560px' })
      .afterClosed()
      .pipe(filter(Boolean))
      .subscribe(() => this.notificarCambio('Mantenimiento finalizado'));
  }

  private notificarCambio(mensaje: string): void {
    this.notificaciones.exito(mensaje);
    this.cambio.emit();
  }
}
