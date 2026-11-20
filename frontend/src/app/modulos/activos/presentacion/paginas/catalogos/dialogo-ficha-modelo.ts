import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

import { EtiquetaPipe } from '../../../../../compartido/pipes/etiqueta.pipe';
import { ModeloActivo, TIPOS_CONTROL, nombreCompletoModelo } from '../../../dominio/modelos/modelo-activo';

/** Ficha de consulta del modelo: que es, su categoria, como se controla y sus especificaciones. */
@Component({
  selector: 'app-dialogo-ficha-modelo',
  imports: [EtiquetaPipe, MatButtonModule, MatDialogModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>{{ nombre }}</h2>
    <mat-dialog-content>
      <p class="texto-secundario">
        {{ modelo.categoria.nombre }} · {{ modelo.tipoControl | etiqueta: tiposControl }}
        @if (modelo.horasMantenimiento) {
          · mantenimiento cada {{ modelo.horasMantenimiento }} h de uso
        }
      </p>
      <h3>Descripción</h3>
      @if (modelo.descripcion) {
        <p class="descripcion">{{ modelo.descripcion }}</p>
      } @else {
        <p class="texto-secundario">Este modelo aún no tiene descripción.</p>
      }
      <h3>Especificaciones técnicas</h3>
      @if (modelo.especificaciones.length) {
        <dl class="datos">
          @for (especificacion of modelo.especificaciones; track especificacion.caracteristica) {
            <dt>{{ especificacion.caracteristica }}</dt>
            <dd>{{ especificacion.valor }}</dd>
          }
        </dl>
      } @else {
        <p class="texto-secundario">Sin especificaciones registradas.</p>
      }
      <p class="texto-secundario">
        En inventario: {{ modelo.cantidadInventario }}
        {{ modelo.tipoControl === 'UNIDAD' ? (modelo.cantidadInventario === 1 ? 'unidad' : 'unidades') : 'en existencia' }}
      </p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-flat-button mat-dialog-close>Cerrar</button>
    </mat-dialog-actions>
  `,
  styles: `
    h3 {
      margin: 16px 0 4px;
      font-size: 14px;
      font-weight: 600;
    }

    .descripcion {
      white-space: pre-line;
      margin: 0;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoFichaModelo {
  protected readonly modelo = inject<ModeloActivo>(MAT_DIALOG_DATA);
  protected readonly nombre = nombreCompletoModelo(this.modelo);
  protected readonly tiposControl = TIPOS_CONTROL;
}
