import { CurrencyPipe, DatePipe, LowerCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

import { EtiquetaPipe } from '../../../../../compartido/pipes/etiqueta.pipe';
import { ESTADOS_MOVIMIENTO, Movimiento, NATURALEZAS } from '../../../dominio/modelos/movimiento';

/** Ficha de solo lectura de un movimiento, con lo vendido y la anulacion. */
@Component({
  selector: 'app-dialogo-detalle-movimiento',
  imports: [CurrencyPipe, DatePipe, EtiquetaPipe, LowerCasePipe, MatButtonModule, MatDialogModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>Movimiento N.º {{ movimiento.id }}</h2>
    <mat-dialog-content>
      @if (movimiento.estado === 'ANULADO') {
        <div class="aviso aviso--peligro" role="status">
          <mat-icon>block</mat-icon><span>Anulado: {{ movimiento.motivoAnulacion }}</span>
        </div>
      }
      <dl class="datos">
        <dt>Tipo</dt>
        <dd>{{ movimiento.tipo.nombre }} ({{ movimiento.tipo.naturaleza | etiqueta: naturalezas | lowercase }})</dd>
        <dt>Concepto</dt>
        <dd>{{ movimiento.concepto }}</dd>
        <dt>Monto</dt>
        <dd><strong>{{ movimiento.monto | currency: 'BOB' : 'Bs ' }}</strong></dd>
        <dt>Fecha</dt>
        <dd>{{ movimiento.fecha | date: 'dd/MM/yyyy' }}</dd>
        <dt>Proyecto</dt>
        <dd>{{ movimiento.proyecto?.nombre ?? 'General de la empresa' }}</dd>
        @if (movimiento.evento) {
          <dt>Evento</dt>
          <dd>{{ movimiento.evento.nombre }}</dd>
        }
        @if (movimiento.proveedor) {
          <dt>Proveedor</dt>
          <dd>{{ movimiento.proveedor.nombre }}</dd>
        }
        <dt>Factura</dt>
        <dd>{{ movimiento.numeroFactura ?? '—' }}</dd>
        <dt>Estado</dt>
        <dd>{{ movimiento.estado | etiqueta: estados }}</dd>
        <dt>Registrado</dt>
        <dd>{{ movimiento.fechaCreacion | date: 'dd/MM/yyyy HH:mm' }} · {{ movimiento.nombreCreador ?? '—' }}</dd>
      </dl>
      @if (movimiento.items.length) {
        <h3>Equipos vendidos</h3>
        <ul>
          @for (item of movimiento.items; track $index) {
            <li>{{ item.idActivo ? '' : item.cantidad + ' × ' }}{{ item.descripcion }}</li>
          }
        </ul>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cerrar</button>
    </mat-dialog-actions>
  `,
  styles: `
    .datos {
      display: grid;
      grid-template-columns: max-content 1fr;
      gap: 6px 16px;
      margin: 0;

      dt {
        color: var(--vc-texto-secundario);
      }

      dd {
        margin: 0;
      }
    }

    .aviso {
      margin-bottom: 12px;
    }

    h3 {
      margin: 16px 0 4px;
      font-size: 14px;
      font-weight: 600;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoDetalleMovimiento {
  protected readonly movimiento = inject<Movimiento>(MAT_DIALOG_DATA);
  protected readonly naturalezas = NATURALEZAS;
  protected readonly estados = ESTADOS_MOVIMIENTO;
}
