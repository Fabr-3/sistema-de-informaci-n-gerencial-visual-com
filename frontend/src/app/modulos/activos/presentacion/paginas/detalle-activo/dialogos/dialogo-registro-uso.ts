import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { aFechaHoraLocal } from '../../../../../../compartido/utilidades/fechas';
import { ServicioNotificaciones } from '../../../../../../nucleo/notificaciones/servicio-notificaciones';
import { HistorialActivos } from '../../../../aplicacion/historial-activos';
import { Activo } from '../../../../dominio/modelos/activo';
import { RegistroUso } from '../../../../dominio/modelos/historial';
import { finPosteriorAInicio, noFutura } from '../../../../../../compartido/utilidades/validadores-periodo';

const LONGITUD_DESCRIPCION = 255;

/** Registra un periodo de uso ya realizado; suma horas para el mantenimiento preventivo. */
@Component({
  selector: 'app-dialogo-registro-uso',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>Registrar uso · {{ activo.codigoInventario }}</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <div class="formulario">
          <mat-form-field appearance="outline">
            <mat-label>Inicio</mat-label>
            <input matInput type="datetime-local" formControlName="fechaInicio" [max]="ahora" />
            <mat-error>Indique un inicio válido</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Fin</mat-label>
            <input matInput type="datetime-local" formControlName="fechaFin" [max]="ahora" />
            <mat-error>El fin no puede ser futuro</mat-error>
          </mat-form-field>
          @if (formulario.hasError('periodoInvalido')) {
            <p class="completo error-grupo" role="alert">El fin debe ser posterior al inicio</p>
          }
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Descripción (opcional)</mat-label>
            <input matInput formControlName="descripcion" placeholder="Evento, cliente o motivo" />
          </mat-form-field>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="guardando()">Registrar</button>
      </mat-dialog-actions>
    </form>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoRegistroUso {
  protected readonly activo = inject<Activo>(MAT_DIALOG_DATA);
  private readonly dialogo = inject(MatDialogRef<DialogoRegistroUso, RegistroUso>);
  private readonly historial = inject(HistorialActivos);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly ahora = aFechaHoraLocal(new Date());
  protected readonly guardando = signal(false);
  protected readonly formulario = inject(NonNullableFormBuilder).group(
    {
      fechaInicio: ['', [Validators.required, noFutura]],
      fechaFin: [this.ahora, [Validators.required, noFutura]],
      descripcion: ['', Validators.maxLength(LONGITUD_DESCRIPCION)],
    },
    { validators: finPosteriorAInicio('fechaInicio', 'fechaFin') },
  );

  protected guardar(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const { fechaInicio, fechaFin, descripcion } = this.formulario.getRawValue();
    this.guardando.set(true);
    this.historial
      .registrarUso(this.activo.id, { fechaInicio, fechaFin, descripcion: descripcion.trim() || null })
      .subscribe({
        next: (registro) => this.dialogo.close(registro),
        error: (error: unknown) => {
          this.notificaciones.error(error);
          this.guardando.set(false);
        },
      });
  }
}
