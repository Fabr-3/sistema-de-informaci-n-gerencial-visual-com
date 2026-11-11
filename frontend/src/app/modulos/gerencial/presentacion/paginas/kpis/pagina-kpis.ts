import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { catchError, filter, map, of, startWith, switchMap } from 'rxjs';

import { aFechaIso } from '../../../../../compartido/utilidades/fechas';
import { Rutas, rutaAbsoluta } from '../../../../../nucleo/configuracion/rutas';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { variacion } from '../../../../finanzas/dominio/modelos/indicadores';
import { GestionGerencial } from '../../../aplicacion/gestion-gerencial';
import {
  KpisGerenciales,
  SentidoCambio,
  TasaUtilizacion,
  UTILIZACION_BAJA,
  anchoBarra,
  cambioEnPuntos,
} from '../../../dominio/modelos/kpis';

interface Cambio {
  readonly texto: string;
  readonly sentido: SentidoCambio;
}

interface TarjetaKpi {
  readonly titulo: string;
  readonly valor: string;
  readonly ayuda: string;
  readonly detalle: string;
  /** Nulo si no hay con que comparar. */
  readonly cambio: Cambio | null;
  readonly sinComparacion?: string;
}

interface FilaResultado {
  readonly concepto: string;
  readonly actual: number;
  readonly anterior: number;
  readonly destacada: boolean;
}

const SIN_DATO = '—';

/**
 * Tablero de informacion gerencial: ganancias, margenes bruto y neto,
 * rentabilidad recurrente anual, utilizacion de recursos y alertas de activos.
 */
@Component({
  selector: 'app-pagina-kpis',
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
    MatTooltipModule,
  ],
  templateUrl: './pagina-kpis.html',
  styleUrl: './pagina-kpis.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaKpis {
  private readonly gestion = inject(GestionGerencial);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly utilizacionBaja = UTILIZACION_BAJA;
  protected readonly anchoBarra = anchoBarra;
  protected readonly rutaAlertas = rutaAbsoluta(Rutas.GERENCIAL, Rutas.ALERTAS);
  protected readonly rutaReportes = rutaAbsoluta(Rutas.GERENCIAL, Rutas.REPORTES);
  protected readonly rango = new FormGroup({
    desde: new FormControl<Date | null>(this.inicioDeMes()),
    hasta: new FormControl<Date | null>(new Date()),
  });

  /** undefined mientras carga; null si fallo. */
  protected readonly kpis = toSignal<KpisGerenciales | null>(
    this.rango.valueChanges.pipe(
      startWith(this.rango.getRawValue()),
      map(() => this.rango.getRawValue()),
      filter(({ desde, hasta }) => !!desde && !!hasta),
      switchMap(({ desde, hasta }) =>
        this.gestion.kpis(aFechaIso(desde!), aFechaIso(hasta!)).pipe(
          catchError((error: unknown) => {
            this.notificaciones.error(error);
            return of(null);
          }),
        ),
      ),
    ),
  );

  protected readonly tarjetas = computed<TarjetaKpi[]>(() => {
    const datos = this.kpis();
    if (!datos) {
      return [];
    }
    const { actual, anterior, rentabilidadRecurrente: recurrente, utilizacion } = datos;
    const cambioGanancia = variacion(actual.ganancia, anterior.ganancia);
    return [
      {
        titulo: 'Ganancias',
        valor: this.moneda(actual.ganancia),
        ayuda: 'Utilidad neta: ingresos menos costos directos y gastos generales.',
        detalle: `Utilidad bruta ${this.moneda(actual.utilidadBruta)}`,
        cambio: cambioGanancia && { texto: `${this.numero(cambioGanancia.porcentaje)} %`, sentido: cambioGanancia.sentido },
      },
      this.tarjetaMargen(
        'Margen de utilidad bruta',
        'Utilidad bruta sobre los ingresos (sin sueldos ni gastos generales).',
        actual.margenBruto,
        anterior.margenBruto,
      ),
      this.tarjetaMargen(
        'Margen de beneficio neto',
        'Ganancia (utilidad neta) sobre los ingresos.',
        actual.margenNeto,
        anterior.margenNeto,
      ),
      {
        titulo: 'Rentabilidad recurrente anual',
        valor: this.porcentaje(recurrente.porcentaje),
        ayuda:
          `Margen sobre ingresos de los proyectos de clientes con ${recurrente.minimoProyectos} o más proyectos ` +
          'contratados, en los últimos 12 meses.',
        detalle:
          `${recurrente.clientesRecurrentes} de ${recurrente.clientesConProyectos} clientes · ` +
          (recurrente.participacion === null
            ? 'sin margen anual positivo'
            : `${this.numero(recurrente.participacion)} % del margen anual`),
        cambio: null,
        sinComparacion: 'Últimos 12 meses',
      },
      {
        titulo: 'Utilización de recursos',
        valor: this.porcentaje(utilizacion.equipos.porcentaje),
        ayuda: 'Días con uso de los equipos sobre los días disponibles desde su adquisición.',
        detalle: `Equipos (personal técnico: ${this.porcentaje(utilizacion.personal.porcentaje)})`,
        cambio: null,
        sinComparacion: 'Del periodo',
      },
    ];
  });

  protected readonly filasResultado = computed<FilaResultado[]>(() => {
    const datos = this.kpis();
    if (!datos) {
      return [];
    }
    const { actual, anterior } = datos;
    return [
      { concepto: 'Ingresos', actual: actual.ingresos, anterior: anterior.ingresos, destacada: false },
      { concepto: 'Costos directos', actual: actual.costosDirectos, anterior: anterior.costosDirectos, destacada: false },
      { concepto: 'Utilidad bruta', actual: actual.utilidadBruta, anterior: anterior.utilidadBruta, destacada: true },
      { concepto: 'Gastos generales', actual: actual.gastosGenerales, anterior: anterior.gastosGenerales, destacada: false },
      { concepto: 'Ganancia (utilidad neta)', actual: actual.ganancia, anterior: anterior.ganancia, destacada: true },
    ];
  });

  protected readonly totalAlertas = computed(() => {
    const alertas = this.kpis()?.alertas;
    return alertas ? alertas.criticas + alertas.advertencias + alertas.informativas : 0;
  });

  protected esBaja(tasa: TasaUtilizacion): boolean {
    return tasa.porcentaje !== null && tasa.porcentaje < UTILIZACION_BAJA;
  }

  protected iconoCambio(sentido: SentidoCambio): string {
    return sentido === 'sube' ? 'arrow_upward' : sentido === 'baja' ? 'arrow_downward' : 'remove';
  }

  private tarjetaMargen(titulo: string, ayuda: string, actual: number | null, anterior: number | null): TarjetaKpi {
    const cambio = cambioEnPuntos(actual, anterior);
    return {
      titulo,
      valor: this.porcentaje(actual),
      ayuda,
      detalle: anterior === null ? '' : `Periodo anterior ${this.porcentaje(anterior)}`,
      cambio: cambio && { texto: `${this.numero(cambio.puntos)} pp`, sentido: cambio.sentido },
      sinComparacion: anterior === null ? 'Sin ingresos en el periodo anterior' : 'Sin ingresos en el periodo',
    };
  }

  private porcentaje(valor: number | null): string {
    return valor === null ? SIN_DATO : `${this.numero(valor)} %`;
  }

  private numero(valor: number): string {
    return valor.toLocaleString('es-BO', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
  }

  private moneda(valor: number): string {
    return `Bs ${valor.toLocaleString('es-BO', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }

  private inicioDeMes(): Date {
    const hoy = new Date();
    return new Date(hoy.getFullYear(), hoy.getMonth(), 1);
  }
}
