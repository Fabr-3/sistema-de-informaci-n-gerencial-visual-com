import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { ServicioNotificaciones } from '../../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionUsuarios } from '../../../../aplicacion/gestion-usuarios';
import { Usuario } from '../../../../dominio/modelos/usuario';
import {
  DESCRIPCION_POLITICA_CONTRASENA,
  politicaContrasena,
} from '../../../validadores/validadores-contrasena';

/** Asigna una contrasena temporal; tambien desbloquea la cuenta. Cierra con true si se aplico. */
@Component({
  selector: 'app-dialogo-restablecer-contrasena',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>Restablecer contraseña</h2>
    <form [formGroup]="formulario" (ngSubmit)="restablecer()" novalidate>
      <mat-dialog-content>
        <p class="indicacion">
          Se asignará una contraseña temporal a <strong>{{ usuario.nombreCompleto }}</strong>. Su cuenta se
          desbloqueará y se cerrarán sus sesiones abiertas.
        </p>
        <mat-form-field appearance="outline" class="campo">
          <mat-label>Contraseña temporal</mat-label>
          <input matInput type="password" formControlName="contrasenaTemporal" autocomplete="new-password" />
          <mat-hint>{{ politica }}</mat-hint>
          <mat-error>No cumple la política de contraseñas</mat-error>
        </mat-form-field>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="guardando()">Restablecer</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .campo {
      width: 100%;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoRestablecerContrasena {
  private readonly dialogo = inject(MatDialogRef<DialogoRestablecerContrasena, boolean>);
  private readonly gestion = inject(GestionUsuarios);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly usuario = inject<Usuario>(MAT_DIALOG_DATA);
  protected readonly politica = DESCRIPCION_POLITICA_CONTRASENA;
  protected readonly guardando = signal(false);
  protected readonly formulario = inject(NonNullableFormBuilder).group({
    contrasenaTemporal: ['', [Validators.required, politicaContrasena]],
  });

  protected restablecer(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.gestion.restablecerContrasena(this.usuario.id, this.formulario.getRawValue().contrasenaTemporal).subscribe({
      next: () => this.dialogo.close(true),
      error: (error: unknown) => {
        this.notificaciones.error(error);
        this.guardando.set(false);
      },
    });
  }
}
