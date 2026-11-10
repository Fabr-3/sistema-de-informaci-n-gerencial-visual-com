import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { catchError, of, switchMap } from 'rxjs';

import { EtiquetaPipe } from '../../../../../compartido/pipes/etiqueta.pipe';
import { Rutas, rutaAbsoluta } from '../../../../../nucleo/configuracion/rutas';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { ESTADOS_PROYECTO, tipoVisualProyecto } from '../../../../comercial/dominio/modelos/proyecto';
import { GestionResultados } from '../../../aplicacion/gestion-resultados';
import { DetalleResultadoProyecto, textoPorcentaje, tipoVisualMargen } from '../../../dominio/modelos/resultado';

const COLUMNAS_EVENTOS = ['evento', 'ingresos', 'costos', 'margen'];
const COLUMNAS_TIPOS = ['tipo', 'monto'];

/** Resultado de un proyecto desglosado por evento y por tipo de movimiento. */
@Component({
  selector: 'app-detalle-resultado',
  imports: [
    CurrencyPipe,
    DatePipe,
    EtiquetaPipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatTableModule,
  ],
  templateUrl: './detalle-resultado.html',
  styleUrl: './resultados.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DetalleResultado {
  /** Parametro de ruta :id (proyecto). */
  readonly id = input.required<string>();

  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly gestion = inject(GestionResultados);

  protected readonly rutaListado = rutaAbsoluta(Rutas.FINANZAS, Rutas.RESULTADOS);
  protected readonly rutaMovimientos = rutaAbsoluta(Rutas.FINANZAS, Rutas.MOVIMIENTOS);
  protected readonly columnasEventos = COLUMNAS_EVENTOS;
  protected readonly columnasTipos = COLUMNAS_TIPOS;
  protected readonly estados = ESTADOS_PROYECTO;
  protected readonly tipoVisualProyecto = tipoVisualProyecto;
  protected readonly tipoVisualMargen = tipoVisualMargen;
  protected readonly textoPorcentaje = textoPorcentaje;

  /** undefined mientras carga; null si fallo. */
  protected readonly detalle = toSignal<DetalleResultadoProyecto | null>(
    toObservable(this.id).pipe(
      switchMap((id) =>
        this.gestion.detalle(Number(id)).pipe(
          catchError((error: unknown) => {
            this.notificaciones.error(error);
            return of(null);
          }),
        ),
      ),
    ),
  );
}
