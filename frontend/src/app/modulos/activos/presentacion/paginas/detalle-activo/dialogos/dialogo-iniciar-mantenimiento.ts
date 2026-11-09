import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { aFechaHoraLocal } from '../../../../../../compartido/utilidades/fechas';
import { ServicioNotificaciones } from '../../../../../../nucleo/notificaciones/servicio-notificaciones';
import { CatalogosActivos } from '../../../../aplicacion/catalogos-activos';
import { HistorialActivos } from '../../../../aplicacion/historial-activos';
import { Activo } from '../../../../dominio/modelos/activo';
import { Mantenimiento } from '../../../../dominio/modelos/historial';
import { noFutura } from '../../../../../../compartido/utilidades/validadores-periodo';

const LONGITUD_DESCRIPCION = 2000;

/** Ingresa el activo a mantenimiento (queda no disponible hasta finalizarlo). */
@Component({
  selector: 'app-dialogo-iniciar-mantenimiento',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>Iniciar mantenimiento · {{ activo.codigoInventario }}</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <p class="indicacion">El activo quedará en mantenimiento y no podrá asignarse hasta finalizarlo.</p>
        <div class="formulario">
          <mat-form-field appearance="outline">
            <mat-label>Tipo</mat-label>
            <mat-select formControlName="idTipoMantenimiento">
              @for (tipo of tipos(); track tipo.id) {
                <mat-option [value]="tipo.id">{{ tipo.nombre }}</mat-option>
              }
            </mat-select>
            <mat-error>Seleccione el tipo</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Inicio</mat-label>
            <input matInput type="datetime-local" formControlName="fechaInicio" [max]="ahora" />
            <mat-error>Indique un inicio que no sea futuro</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Descripción del trabajo (opcional)</mat-label>
            <textarea matInput rows="3" formControlName="descripcion"></textarea>
          </mat-form-field>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="guardando()">Iniciar</button>
      </mat-dialog-actions>
    </form>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoIniciarMantenimiento {
  protected readonly activo = inject<Activo>(MAT_DIALOG_DATA);
  private readonly dialogo = inject(MatDialogRef<DialogoIniciarMantenimiento, Mantenimiento>);
  private readonly historial = inject(HistorialActivos);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly tipos = toSignal(inject(CatalogosActivos).listarTiposMantenimiento(), { initialValue: [] });
  protected readonly ahora = aFechaHoraLocal(new Date());
  protected readonly guardando = signal(false);
  protected readonly formulario = inject(NonNullableFormBuilder).group({
    idTipoMantenimiento: [0, Validators.min(1)],
    fechaInicio: [this.ahora, [Validators.required, noFutura]],
    descripcion: ['', Validators.maxLength(LONGITUD_DESCRIPCION)],
  });

  protected guardar(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const { idTipoMantenimiento, fechaInicio, descripcion } = this.formulario.getRawValue();
    this.guardando.set(true);
    this.historial
      .iniciarMantenimiento(this.activo.id, { idTipoMantenimiento, fechaInicio, descripcion: descripcion.trim() || null })
      .subscribe({
        next: (mantenimiento) => this.dialogo.close(mantenimiento),
        error: (error: unknown) => {
          this.notificaciones.error(error);
          this.guardando.set(false);
        },
      });
  }
}
