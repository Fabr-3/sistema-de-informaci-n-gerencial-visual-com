import { CurrencyPipe, DatePipe, DecimalPipe, KeyValuePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';

import { EtiquetaPipe } from '../../../../../compartido/pipes/etiqueta.pipe';
import { Rutas, rutaAbsoluta } from '../../../../../nucleo/configuracion/rutas';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionComercial } from '../../../aplicacion/gestion-comercial';
import { Cliente, ResumenComercialCliente, tasaConversion } from '../../../dominio/modelos/cliente';
import { ESTADOS_COTIZACION, tipoVisualCotizacion } from '../../../dominio/modelos/cotizacion';
import { ESTADOS_PROYECTO, tipoVisualProyecto } from '../../../dominio/modelos/proyecto';

/** Cotizaciones y proyectos del cliente, con enlaces a cada uno (extension de «Consultar cliente»). */
@Component({
  selector: 'app-dialogo-resumen-cliente',
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    EtiquetaPipe,
    KeyValuePipe,
    RouterLink,
    MatButtonModule,
    MatDialogModule,
    MatProgressBarModule,
  ],
  template: `
    <h2 mat-dialog-title>Resumen comercial · {{ cliente.razonSocial }}</h2>
    <mat-dialog-content>
      @if (resumen() === undefined) {
        <mat-progress-bar mode="indeterminate" />
      } @else if (resumen(); as datos) {
        <dl class="cifras">
          <div>
            <dt>Cotizado</dt>
            <dd>{{ datos.importeCotizado | currency: 'BOB' : 'Bs ' }}</dd>
          </div>
          <div>
            <dt>Contratado</dt>
            <dd>{{ datos.importeContratado | currency: 'BOB' : 'Bs ' }}</dd>
          </div>
          <div>
            <dt>Conversión</dt>
            <dd>{{ tasaConversion(datos) === null ? '—' : (tasaConversion(datos) | number: '1.0-1') + ' %' }}</dd>
          </div>
        </dl>

        <h3>Cotizaciones</h3>
        <p class="estados">
          @for (estado of datos.cotizacionesPorEstado | keyvalue; track estado.key) {
            <span class="estado estado--{{ tipoVisualCotizacion($any(estado.key)) }}">
              {{ estado.key | etiqueta: estadosCotizacion }}: {{ estado.value }}
            </span>
          } @empty {
            <span class="texto-secundario">Sin cotizaciones.</span>
          }
        </p>
        <ul class="recientes">
          @for (cotizacion of datos.ultimasCotizaciones; track cotizacion.id) {
            <li>
              <a [routerLink]="rutaCotizacion(cotizacion.id)" mat-dialog-close>{{ cotizacion.referencia }}</a>
              <span class="texto-secundario">
                · {{ cotizacion.estado | etiqueta: estadosCotizacion }} · {{ cotizacion.fecha | date: 'dd/MM/yyyy' }}
              </span>
            </li>
          }
        </ul>

        <h3>Proyectos</h3>
        <p class="estados">
          @for (estado of datos.proyectosPorEstado | keyvalue; track estado.key) {
            <span class="estado estado--{{ tipoVisualProyecto($any(estado.key)) }}">
              {{ estado.key | etiqueta: estadosProyecto }}: {{ estado.value }}
            </span>
          } @empty {
            <span class="texto-secundario">Sin proyectos.</span>
          }
        </p>
        <ul class="recientes">
          @for (proyecto of datos.ultimosProyectos; track proyecto.id) {
            <li>
              <a [routerLink]="rutaProyecto(proyecto.id)" mat-dialog-close>{{ proyecto.referencia }}</a>
              <span class="texto-secundario">
                · {{ proyecto.estado | etiqueta: estadosProyecto }}
                @if (proyecto.fecha) {
                  · {{ proyecto.fecha | date: 'dd/MM/yyyy' }}
                }
              </span>
            </li>
          }
        </ul>
      } @else {
        <p class="texto-secundario">No se pudo cargar el resumen comercial.</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>Cerrar</button>
    </mat-dialog-actions>
  `,
  styles: `
    .cifras {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
      gap: 12px;
      margin: 0 0 8px;
    }

    .cifras dt {
      color: var(--vc-texto-secundario);
      font-size: 0.85rem;
    }

    .cifras dd {
      margin: 0;
      font-weight: 600;
    }

    h3 {
      margin: 16px 0 8px;
      font-size: 1rem;
    }

    .estados {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin: 0 0 8px;
    }

    .recientes {
      margin: 0;
      padding-left: 18px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoResumenCliente {
  protected readonly cliente = inject<Cliente>(MAT_DIALOG_DATA);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly estadosCotizacion = ESTADOS_COTIZACION;
  protected readonly estadosProyecto = ESTADOS_PROYECTO;
  protected readonly tipoVisualCotizacion = tipoVisualCotizacion;
  protected readonly tipoVisualProyecto = tipoVisualProyecto;
  protected readonly tasaConversion = tasaConversion;
  /** undefined mientras carga; null si fallo. */
  protected readonly resumen = toSignal<ResumenComercialCliente | null>(
    inject(GestionComercial)
      .resumenCliente(this.cliente.id)
      .pipe(
        catchError((error: unknown) => {
          this.notificaciones.error(error);
          return of(null);
        }),
      ),
  );

  protected rutaCotizacion(id: number): string {
    return rutaAbsoluta(Rutas.COMERCIAL, Rutas.COTIZACIONES, String(id));
  }

  protected rutaProyecto(id: number): string {
    return rutaAbsoluta(Rutas.COMERCIAL, Rutas.PROYECTOS, String(id));
  }
}
