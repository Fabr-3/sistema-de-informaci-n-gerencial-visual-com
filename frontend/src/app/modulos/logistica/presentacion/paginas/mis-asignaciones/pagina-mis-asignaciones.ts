import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { catchError, of } from 'rxjs';

import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionPlanificacion } from '../../../aplicacion/gestion-planificacion';
import { MisAsignaciones } from '../../../dominio/modelos/personal';

/** Proximas asignaciones del tecnico o sub-contratado que inicio sesion. */
@Component({
  selector: 'app-pagina-mis-asignaciones',
  imports: [DatePipe, MatIconModule, MatProgressBarModule],
  templateUrl: './pagina-mis-asignaciones.html',
  styleUrl: './pagina-mis-asignaciones.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaMisAsignaciones {
  private readonly notificaciones = inject(ServicioNotificaciones);

  /** undefined mientras carga; null si fallo la consulta. */
  protected readonly resultado = toSignal<MisAsignaciones | null>(
    inject(GestionPlanificacion)
      .misAsignaciones()
      .pipe(
        catchError((error: unknown) => {
          this.notificaciones.error(error);
          return of(null);
        }),
      ),
  );
}
