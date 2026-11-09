import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { finalize } from 'rxjs';

import { descargarArchivo, formatearTamano } from '../../../../../compartido/utilidades/archivos';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionRespaldos } from '../../../aplicacion/gestion-respaldos';
import { Respaldo } from '../../../dominio/modelos/respaldo';

/** Copias de seguridad de la base de datos (manuales y diarias automaticas). */
@Component({
  selector: 'app-pagina-respaldos',
  imports: [DatePipe, MatButtonModule, MatIconModule, MatProgressBarModule, MatTableModule, MatTooltipModule],
  templateUrl: './pagina-respaldos.html',
  styleUrl: './pagina-respaldos.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaRespaldos {
  private readonly gestion = inject(GestionRespaldos);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly columnas = ['nombreArchivo', 'fechaCreacion', 'tamano', 'acciones'];
  protected readonly formatearTamano = formatearTamano;
  protected readonly respaldos = signal<Respaldo[]>([]);
  protected readonly cargando = signal(true);
  protected readonly generando = signal(false);
  protected readonly descargando = signal<string | null>(null);

  constructor() {
    this.gestion
      .listar()
      .pipe(
        finalize(() => this.cargando.set(false)),
        takeUntilDestroyed(),
      )
      .subscribe({
        next: (respaldos) => this.respaldos.set(respaldos),
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  protected generar(): void {
    this.generando.set(true);
    this.gestion
      .generar()
      .pipe(finalize(() => this.generando.set(false)))
      .subscribe({
        next: (respaldo) => {
          this.respaldos.update((actuales) => [respaldo, ...actuales]);
          this.notificaciones.exito('Respaldo generado correctamente');
        },
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  protected descargar(respaldo: Respaldo): void {
    this.descargando.set(respaldo.nombreArchivo);
    this.gestion
      .descargar(respaldo)
      .pipe(finalize(() => this.descargando.set(null)))
      .subscribe({
        next: (contenido) => descargarArchivo(contenido, respaldo.nombreArchivo),
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }
}
