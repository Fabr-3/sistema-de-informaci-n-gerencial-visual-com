import { KeyValuePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { aFechaHoraLocal } from '../../../../../../compartido/utilidades/fechas';
import { ServicioNotificaciones } from '../../../../../../nucleo/notificaciones/servicio-notificaciones';
import { HistorialActivos } from '../../../../aplicacion/historial-activos';
import { CONDICIONES_FINALES, CondicionFinal, Mantenimiento } from '../../../../dominio/modelos/historial';
import { noFutura } from '../../../../../../compartido/utilidades/validadores-periodo';

const LONGITUD_OBSERVACIONES = 2000;

/** Cierra el mantenimiento con su resultado; un activo no reparable se da de baja. */
@Component({
  selector: 'app-dialogo-finalizar-mantenimiento',
  imports: [
    KeyValuePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  template: `
    <h2 mat-dialog-title>Finalizar mantenimiento</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <div class="formulario">
          <mat-form-field appearance="outline">
            <mat-label>Fin</mat-label>
            <input matInput type="datetime-local" formControlName="fechaFin" [min]="minimo" [max]="ahora" />
            <mat-error>Debe estar entre el inicio y la fecha actual</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Costo en Bs (opcional)</mat-label>
            <input matInput type="number" min="0" step="0.01" formControlName="costo" />
            <mat-error>El costo no puede ser negativo</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Condición final del activo</mat-label>
            <mat-select formControlName="condicionFinal">
              @for (condicion of condiciones | keyvalue: ordenOriginal; track condicion.key) {
                <mat-option [value]="condicion.key">{{ condicion.value }}</mat-option>
              }
            </mat-select>
            <mat-error>Seleccione la condición final</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Observaciones (opcional)</mat-label>
            <textarea matInput rows="3" formControlName="observaciones"></textarea>
          </mat-form-field>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="guardando()">Finalizar</button>
      </mat-dialog-actions>
    </form>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoFinalizarMantenimiento {
  protected readonly mantenimiento = inject<Mantenimiento>(MAT_DIALOG_DATA);
  private readonly dialogo = inject(MatDialogRef<DialogoFinalizarMantenimiento, Mantenimiento>);
  private readonly historial = inject(HistorialActivos);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly condiciones = CONDICIONES_FINALES;
  protected readonly ordenOriginal = (): number => 0;
  protected readonly ahora = aFechaHoraLocal(new Date());
  protected readonly minimo = this.mantenimiento.fechaInicio.substring(0, 16);
  protected readonly guardando = signal(false);
  protected readonly formulario = inject(NonNullableFormBuilder).group({
    fechaFin: [this.ahora, [Validators.required, noFutura]],
    condicionFinal: [null as CondicionFinal | null, Validators.required],
    observaciones: ['', Validators.maxLength(LONGITUD_OBSERVACIONES)],
    costo: [null as number | null, Validators.min(0)],
  });

  protected guardar(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const { fechaFin, condicionFinal, observaciones, costo } = this.formulario.getRawValue();
    this.guardando.set(true);
    this.historial
      .finalizarMantenimiento(this.mantenimiento.idActivo, this.mantenimiento.id, {
        fechaFin,
        condicionFinal: condicionFinal!,
        observaciones: observaciones.trim() || null,
        costo,
      })
      .subscribe({
        next: (finalizado) => this.dialogo.close(finalizado),
        error: (error: unknown) => {
          this.notificaciones.error(error);
          this.guardando.set(false);
        },
      });
  }
}
