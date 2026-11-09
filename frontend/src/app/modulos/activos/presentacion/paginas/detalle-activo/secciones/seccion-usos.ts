import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, combineLatest, filter, of, switchMap } from 'rxjs';

import {
  DatosConfirmacion,
  DialogoConfirmacion,
} from '../../../../../../compartido/componentes/dialogo-confirmacion/dialogo-confirmacion';
import { Pagina, TAMANO_PAGINA_POR_DEFECTO } from '../../../../../../compartido/modelos/pagina';
import { ServicioNotificaciones } from '../../../../../../nucleo/notificaciones/servicio-notificaciones';
import { ServicioSesion } from '../../../../../accesos/aplicacion/servicio-sesion';
import { Permisos } from '../../../../../accesos/dominio/modelos/permisos';
import { HistorialActivos } from '../../../../aplicacion/historial-activos';
import { Activo, reglasActivo } from '../../../../dominio/modelos/activo';
import { RegistroUso, horasEntre } from '../../../../dominio/modelos/historial';
import { DialogoRegistroUso } from '../dialogos/dialogo-registro-uso';

/** Historial de uso del activo con registro y anulacion de periodos. */
@Component({
  selector: 'app-seccion-usos',
  imports: [DatePipe, DecimalPipe, MatButtonModule, MatIconModule, MatPaginatorModule, MatTableModule, MatTooltipModule],
  templateUrl: './seccion-usos.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SeccionUsos {
  readonly activo = input.required<Activo>();
  /** Avisa que cambiaron las horas del activo. */
  readonly cambio = output<void>();

  private readonly historial = inject(HistorialActivos);
  private readonly sesion = inject(ServicioSesion);
  private readonly dialogos = inject(MatDialog);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly horasEntre = horasEntre;
  protected readonly pagina = signal(0);
  protected readonly registros = signal<Pagina<RegistroUso> | null>(null);
  protected readonly tamano = TAMANO_PAGINA_POR_DEFECTO;
  protected readonly puedeRegistrar = computed(
    () => this.sesion.tienePermiso(Permisos.MANTENIMIENTOS_REGISTRAR) && reglasActivo.puedeRegistrarUso(this.activo()),
  );
  protected readonly puedeAnular = computed(() =>
    this.sesion.tieneAlgunPermiso(Permisos.ACTIVOS_GESTIONAR, Permisos.MANTENIMIENTOS_REGISTRAR),
  );
  protected readonly columnas = computed(() => {
    const base = ['inicio', 'fin', 'horas', 'descripcion', 'registradoPor'];
    return this.puedeAnular() ? [...base, 'acciones'] : base;
  });

  constructor() {
    combineLatest([toObservable(this.activo), toObservable(this.pagina)])
      .pipe(
        switchMap(([activo, pagina]) =>
          this.historial.listarUsos(activo.id, { pagina, tamano: this.tamano }).pipe(
            catchError((error: unknown) => {
              this.notificaciones.error(error);
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((registros) => this.registros.set(registros));
  }

  protected cambiarPagina(evento: PageEvent): void {
    this.pagina.set(evento.pageIndex);
  }

  protected registrar(): void {
    this.dialogos
      .open(DialogoRegistroUso, { data: this.activo(), width: '560px' })
      .afterClosed()
      .pipe(filter(Boolean))
      .subscribe(() => this.notificarCambio('Uso registrado'));
  }

  protected anular(registro: RegistroUso): void {
    this.dialogos
      .open<DialogoConfirmacion, DatosConfirmacion, boolean>(DialogoConfirmacion, {
        data: {
          titulo: 'Anular registro de uso',
          mensaje: 'El periodo dejará de contar para las horas de uso del activo. La anulación queda en la auditoría.',
          textoConfirmar: 'Anular',
          destructiva: true,
        },
        width: '440px',
      })
      .afterClosed()
      .pipe(
        filter((confirmado) => confirmado === true),
        switchMap(() => this.historial.anularUso(this.activo().id, registro.id)),
      )
      .subscribe({
        next: () => this.notificarCambio('Registro anulado'),
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  /** El padre recarga el activo; al recibirlo de nuevo se vuelve a consultar el historial. */
  private notificarCambio(mensaje: string): void {
    this.notificaciones.exito(mensaje);
    this.cambio.emit();
  }
}
