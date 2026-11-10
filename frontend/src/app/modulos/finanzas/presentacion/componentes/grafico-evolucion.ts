import { CurrencyPipe, formatDate } from '@angular/common';
import { ChangeDetectionStrategy, Component, LOCALE_ID, computed, inject, input, signal } from '@angular/core';

import { TotalMensual } from '../../dominio/modelos/indicadores';

/** Lienzo del grafico en unidades del viewBox (se escala al ancho disponible). */
const ANCHO = 720;
const ALTO = 240;
const MARGEN = { superior: 12, derecho: 8, inferior: 28, izquierdo: 64 } as const;
const ANCHO_BARRA_MAXIMO = 18;
const SEPARACION_BARRAS = 2;
const RADIO_EXTREMO = 4;
const MARCAS_EJE = 4;
/** Pasos que dividen en cuatro marcas legibles. */
const PASOS_REDONDEO = [1, 1.2, 1.6, 2, 2.4, 3, 4, 5, 6, 8, 10];
/** El detalle se mantiene dentro del lienzo aunque el mes este en un extremo (% del ancho). */
const POSICION_DETALLE = { minima: 14, maxima: 86 } as const;

interface BarraMes {
  readonly mes: TotalMensual;
  readonly etiqueta: string;
  readonly etiquetaLarga: string;
  readonly xBanda: number;
  readonly anchoBanda: number;
  readonly ingresos: string;
  readonly egresos: string;
}

/** Maximo "redondo" del eje: el menor paso de PASOS_REDONDEO por potencia de diez que cubre el valor. */
export function maximoRedondo(valor: number): number {
  if (valor <= 0) {
    return 1;
  }
  const potencia = 10 ** Math.floor(Math.log10(valor));
  const paso = PASOS_REDONDEO.find((candidato) => candidato * potencia >= valor) ?? 10;
  return paso * potencia;
}

/** Barra vertical con el extremo de datos redondeado y la base recta. */
export function trazoBarra(x: number, ancho: number, base: number, altura: number): string {
  if (altura <= 0) {
    return '';
  }
  const radio = Math.min(RADIO_EXTREMO, ancho / 2, altura);
  const tope = base - altura;
  return (
    `M${x},${base} V${tope + radio} Q${x},${tope} ${x + radio},${tope} ` +
    `H${x + ancho - radio} Q${x + ancho},${tope} ${x + ancho},${tope + radio} V${base} Z`
  );
}

/**
 * Ingresos y egresos por mes en barras agrupadas sobre un solo eje. Cada mes
 * muestra su detalle al pasar el cursor o al enfocarlo con el teclado, y una
 * tabla equivalente queda disponible para lectores de pantalla.
 */
@Component({
  selector: 'app-grafico-evolucion',
  imports: [CurrencyPipe],
  template: `
    <div class="leyenda" aria-hidden="true">
      <span><i class="muestra muestra--ingresos"></i>Ingresos</span>
      <span><i class="muestra muestra--egresos"></i>Egresos</span>
    </div>
    <div class="lienzo">
      <svg [attr.viewBox]="'0 0 ' + ancho + ' ' + alto" role="img" aria-label="Ingresos y egresos de los últimos 12 meses">
        @for (marca of marcas(); track marca.valor) {
          <line class="guia" [attr.x1]="margen.izquierdo" [attr.x2]="ancho - margen.derecho" [attr.y1]="marca.y" [attr.y2]="marca.y" />
          <text class="eje" [attr.x]="margen.izquierdo - 8" [attr.y]="marca.y + 4" text-anchor="end">{{ marca.texto }}</text>
        }
        @for (barra of barras(); track barra.mes.mes; let i = $index) {
          @if (activo() === i) {
            <rect class="resalte" [attr.x]="barra.xBanda" [attr.y]="margen.superior" [attr.width]="barra.anchoBanda" [attr.height]="base - margen.superior" />
          }
          <path class="serie serie--ingresos" [attr.d]="barra.ingresos" />
          <path class="serie serie--egresos" [attr.d]="barra.egresos" />
          <text class="eje" [attr.x]="barra.xBanda + barra.anchoBanda / 2" [attr.y]="alto - 8" text-anchor="middle">{{ barra.etiqueta }}</text>
          <rect
            class="zona"
            [attr.x]="barra.xBanda"
            [attr.y]="0"
            [attr.width]="barra.anchoBanda"
            [attr.height]="alto"
            tabindex="0"
            [attr.aria-label]="barra.etiquetaLarga"
            (mouseenter)="activo.set(i)"
            (mouseleave)="activo.set(null)"
            (focus)="activo.set(i)"
            (blur)="activo.set(null)"
          />
        }
        <line class="base" [attr.x1]="margen.izquierdo" [attr.x2]="ancho - margen.derecho" [attr.y1]="base" [attr.y2]="base" />
      </svg>
      @if (detalle(); as mes) {
        <div class="tooltip" role="status" [style.left.%]="mes.posicion">
          <strong>{{ mes.barra.etiquetaLarga }}</strong>
          <span><i class="muestra muestra--ingresos"></i>Ingresos {{ mes.barra.mes.ingresos | currency: 'BOB' : 'Bs ' }}</span>
          <span><i class="muestra muestra--egresos"></i>Egresos {{ mes.barra.mes.egresos | currency: 'BOB' : 'Bs ' }}</span>
          <span class="saldo">Saldo {{ mes.barra.mes.ingresos - mes.barra.mes.egresos | currency: 'BOB' : 'Bs ' }}</span>
        </div>
      }
    </div>
    <table class="cdk-visually-hidden">
      <caption>Ingresos y egresos por mes</caption>
      <thead><tr><th scope="col">Mes</th><th scope="col">Ingresos</th><th scope="col">Egresos</th></tr></thead>
      <tbody>
        @for (barra of barras(); track barra.mes.mes) {
          <tr>
            <th scope="row">{{ barra.etiquetaLarga }}</th>
            <td>{{ barra.mes.ingresos | currency: 'BOB' : 'Bs ' }}</td>
            <td>{{ barra.mes.egresos | currency: 'BOB' : 'Bs ' }}</td>
          </tr>
        }
      </tbody>
    </table>
  `,
  styleUrl: './grafico-evolucion.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GraficoEvolucion {
  readonly datos = input.required<readonly TotalMensual[]>();

  private readonly locale = inject(LOCALE_ID);
  private readonly formatoEje = new Intl.NumberFormat('es-BO', { notation: 'compact', maximumFractionDigits: 1 });

  protected readonly ancho = ANCHO;
  protected readonly alto = ALTO;
  protected readonly margen = MARGEN;
  protected readonly base = ALTO - MARGEN.inferior;
  protected readonly activo = signal<number | null>(null);

  private readonly maximo = computed(() =>
    maximoRedondo(Math.max(0, ...this.datos().flatMap((mes) => [mes.ingresos, mes.egresos]))),
  );

  protected readonly marcas = computed(() => {
    const alturaUtil = this.base - MARGEN.superior;
    return Array.from({ length: MARCAS_EJE + 1 }, (_, indice) => {
      const valor = (this.maximo() / MARCAS_EJE) * indice;
      return { valor, y: this.base - (alturaUtil * indice) / MARCAS_EJE, texto: `Bs ${this.formatoEje.format(valor)}` };
    });
  });

  protected readonly barras = computed<BarraMes[]>(() => {
    const meses = this.datos();
    const anchoBanda = (ANCHO - MARGEN.izquierdo - MARGEN.derecho) / Math.max(meses.length, 1);
    const anchoBarra = Math.min(ANCHO_BARRA_MAXIMO, (anchoBanda - 8) / 2 - SEPARACION_BARRAS / 2);
    const alturaUtil = this.base - MARGEN.superior;
    const escala = (valor: number) => (valor / this.maximo()) * alturaUtil;
    return meses.map((mes, indice) => {
      const xBanda = MARGEN.izquierdo + anchoBanda * indice;
      const xIngresos = xBanda + anchoBanda / 2 - anchoBarra - SEPARACION_BARRAS / 2;
      const fecha = `${mes.mes}-01`;
      return {
        mes,
        etiqueta: formatDate(fecha, 'MMM', this.locale).replace('.', ''),
        etiquetaLarga: formatDate(fecha, 'MMMM yyyy', this.locale),
        xBanda,
        anchoBanda,
        ingresos: trazoBarra(xIngresos, anchoBarra, this.base, escala(mes.ingresos)),
        egresos: trazoBarra(xIngresos + anchoBarra + SEPARACION_BARRAS, anchoBarra, this.base, escala(mes.egresos)),
      };
    });
  });

  /** Detalle del mes activo, posicionado sobre su banda (en % del ancho). */
  protected readonly detalle = computed(() => {
    const indice = this.activo();
    const barra = indice === null ? null : this.barras()[indice];
    if (!barra) {
      return null;
    }
    const centro = ((barra.xBanda + barra.anchoBanda / 2) / ANCHO) * 100;
    return { barra, posicion: Math.min(POSICION_DETALLE.maxima, Math.max(POSICION_DETALLE.minima, centro)) };
  });
}
