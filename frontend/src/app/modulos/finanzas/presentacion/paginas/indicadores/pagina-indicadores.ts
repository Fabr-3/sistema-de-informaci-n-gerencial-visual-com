import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { catchError, filter, map, of, startWith, switchMap } from 'rxjs';

import { aFechaIso } from '../../../../../compartido/utilidades/fechas';
import { Rutas, rutaAbsoluta } from '../../../../../nucleo/configuracion/rutas';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionIndicadores } from '../../../aplicacion/gestion-indicadores';
import { IndicadoresFinancieros, Variacion, extremosDeMargen, variacion } from '../../../dominio/modelos/indicadores';
import { MARGEN_MINIMO_SALUDABLE, textoPorcentaje, tipoVisualMargen } from '../../../dominio/modelos/resultado';
import { GraficoEvolucion } from '../../componentes/grafico-evolucion';

interface Tarjeta {
  readonly titulo: string;
  readonly valor: number;
  readonly variacion: Variacion | null;
  /** Si subir es bueno (ingresos, margen) o malo (costos, gastos). */
  readonly subirEsBueno: boolean;
  readonly detalle?: string;
}

/**
 * Tablero financiero del gerente: periodo frente al anterior, evolucion
 * mensual, margen por proyecto, cuentas por cobrar y movimientos destacados.
 */
@Component({
  selector: 'app-pagina-indicadores',
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    RouterLink,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressBarModule,
    GraficoEvolucion,
  ],
  templateUrl: './pagina-indicadores.html',
  styleUrl: './pagina-indicadores.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaIndicadores {
  private readonly gestion = inject(GestionIndicadores);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly margenMinimo = MARGEN_MINIMO_SALUDABLE;
  protected readonly tipoVisualMargen = tipoVisualMargen;
  protected readonly textoPorcentaje = textoPorcentaje;
  protected readonly rango = new FormGroup({
    desde: new FormControl<Date | null>(this.inicioDeMes()),
    hasta: new FormControl<Date | null>(new Date()),
  });

  /** undefined mientras carga; null si fallo. */
  protected readonly indicadores = toSignal<IndicadoresFinancieros | null>(
    this.rango.valueChanges.pipe(
      startWith(this.rango.getRawValue()),
      map(() => this.rango.getRawValue()),
      filter(({ desde, hasta }) => !!desde && !!hasta),
      switchMap(({ desde, hasta }) =>
        this.gestion.calcular(aFechaIso(desde!), aFechaIso(hasta!)).pipe(
          catchError((error: unknown) => {
            this.notificaciones.error(error);
            return of(null);
          }),
        ),
      ),
    ),
  );

  protected readonly tarjetas = computed<Tarjeta[]>(() => {
    const datos = this.indicadores();
    if (!datos) {
      return [];
    }
    const { actual, anterior } = datos;
    return [
      { titulo: 'Ingresos', valor: actual.ingresos, variacion: variacion(actual.ingresos, anterior.ingresos), subirEsBueno: true },
      {
        titulo: 'Costos directos',
        valor: actual.costosDirectos,
        variacion: variacion(actual.costosDirectos, anterior.costosDirectos),
        subirEsBueno: false,
      },
      {
        titulo: 'Margen bruto',
        valor: actual.margenBruto.margen,
        variacion: variacion(actual.margenBruto.margen, anterior.margenBruto.margen),
        subirEsBueno: true,
        detalle: textoPorcentaje(actual.margenBruto),
      },
      {
        titulo: 'Gastos generales',
        valor: actual.gastosGenerales,
        variacion: variacion(actual.gastosGenerales, anterior.gastosGenerales),
        subirEsBueno: false,
      },
      {
        titulo: 'Resultado neto',
        valor: actual.resultadoNeto,
        variacion: variacion(actual.resultadoNeto, anterior.resultadoNeto),
        subirEsBueno: true,
      },
    ];
  });

  protected readonly extremos = computed(() =>
    extremosDeMargen(this.indicadores()?.margenProyectos ?? [], MARGEN_MINIMO_SALUDABLE),
  );

  protected rutaResultado(idProyecto: number): string {
    return rutaAbsoluta(Rutas.FINANZAS, Rutas.RESULTADOS, String(idProyecto));
  }

  /** Verde si el cambio es favorable, rojo si es desfavorable. */
  protected claseVariacion(tarjeta: Tarjeta): string {
    if (!tarjeta.variacion || tarjeta.variacion.sentido === 'igual') {
      return 'variacion--neutra';
    }
    const favorable = (tarjeta.variacion.sentido === 'sube') === tarjeta.subirEsBueno;
    return favorable ? 'variacion--favorable' : 'variacion--desfavorable';
  }

  private inicioDeMes(): Date {
    const hoy = new Date();
    return new Date(hoy.getFullYear(), hoy.getMonth(), 1);
  }
}
