import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { finalize } from 'rxjs';

import { descargarArchivo } from '../../../../../compartido/utilidades/archivos';
import { aFechaIso } from '../../../../../compartido/utilidades/fechas';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionGerencial } from '../../../aplicacion/gestion-gerencial';
import { DescripcionReporte, REPORTES, TipoReporte, nombreArchivoReporte } from '../../../dominio/modelos/reportes';

/** Descarga de los reportes operativos en PDF para el periodo elegido. */
@Component({
  selector: 'app-pagina-reportes',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './pagina-reportes.html',
  styleUrl: './pagina-reportes.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaReportes {
  private readonly gestion = inject(GestionGerencial);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly reportes = REPORTES;
  protected readonly rango = new FormGroup({
    desde: new FormControl<Date | null>(this.inicioDeMes(), Validators.required),
    hasta: new FormControl<Date | null>(new Date(), Validators.required),
  });
  /** Reporte que se esta generando (uno a la vez). */
  protected readonly generando = signal<TipoReporte | null>(null);

  protected descargar(reporte: DescripcionReporte): void {
    const { desde, hasta } = this.rango.getRawValue();
    if (reporte.usaPeriodo && (this.rango.invalid || !desde || !hasta)) {
      this.rango.markAllAsTouched();
      return;
    }
    const hoy = aFechaIso(new Date());
    const fechaDesde = desde ? aFechaIso(desde) : hoy;
    const fechaHasta = hasta ? aFechaIso(hasta) : hoy;
    this.generando.set(reporte.tipo);
    this.gestion
      .reporte(reporte.tipo, fechaDesde, fechaHasta)
      .pipe(finalize(() => this.generando.set(null)))
      .subscribe({
        next: (contenido) => descargarArchivo(contenido, nombreArchivoReporte(reporte, fechaDesde, fechaHasta, hoy)),
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  private inicioDeMes(): Date {
    const hoy = new Date();
    return new Date(hoy.getFullYear(), hoy.getMonth(), 1);
  }
}
