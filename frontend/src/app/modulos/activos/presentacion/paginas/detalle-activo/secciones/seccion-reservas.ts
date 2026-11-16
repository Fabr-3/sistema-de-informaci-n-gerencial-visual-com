import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { MatTableModule } from '@angular/material/table';
import { catchError, of, switchMap } from 'rxjs';

import { ServicioNotificaciones } from '../../../../../../nucleo/notificaciones/servicio-notificaciones';
import { HistorialActivos } from '../../../../aplicacion/historial-activos';
import { Activo } from '../../../../dominio/modelos/activo';
import { ReservaVigente } from '../../../../dominio/modelos/historial';

const COLUMNAS = ['evento', 'salida', 'retorno'];

/** Reservas del equipo para eventos proximos; se gestionan desde el plan logistico del evento. */
@Component({
  selector: 'app-seccion-reservas',
  imports: [DatePipe, MatTableModule],
  template: `
    <p class="texto-secundario">
      Las reservas se registran y liberan desde el plan logístico de cada evento.
    </p>
    <div class="tabla-contenedor">
      <table mat-table [dataSource]="reservas()">
        <ng-container matColumnDef="evento">
          <th mat-header-cell *matHeaderCellDef>Evento</th>
          <td mat-cell *matCellDef="let reserva"><strong>{{ reserva.evento }}</strong></td>
        </ng-container>
        <ng-container matColumnDef="salida">
          <th mat-header-cell *matHeaderCellDef>Salida</th>
          <td mat-cell *matCellDef="let reserva">{{ reserva.salida | date: 'dd/MM/yyyy HH:mm' }}</td>
        </ng-container>
        <ng-container matColumnDef="retorno">
          <th mat-header-cell *matHeaderCellDef>Retorno</th>
          <td mat-cell *matCellDef="let reserva">{{ reserva.retorno | date: 'dd/MM/yyyy HH:mm' }}</td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="columnas"></tr>
        <tr mat-row *matRowDef="let fila; columns: columnas"></tr>
        <tr class="mat-row" *matNoDataRow>
          <td class="vacio" [attr.colspan]="columnas.length">El equipo no tiene reservas próximas.</td>
        </tr>
      </table>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SeccionReservas {
  readonly activo = input.required<Activo>();

  private readonly historial = inject(HistorialActivos);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly columnas = COLUMNAS;
  protected readonly reservas = signal<ReservaVigente[]>([]);

  constructor() {
    toObservable(this.activo)
      .pipe(
        switchMap((activo) =>
          this.historial.listarReservas(activo.id).pipe(
            catchError((error: unknown) => {
              this.notificaciones.error(error);
              return of<ReservaVigente[]>([]);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((reservas) => this.reservas.set(reservas));
  }
}
