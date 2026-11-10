import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { guardarYCerrar } from '../../../../../compartido/utilidades/guardar-y-cerrar';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionProveedores } from '../../../aplicacion/gestion-proveedores';
import { Proveedor, separarServicios } from '../../../dominio/modelos/proveedor';

const LONGITUD_RAZON_SOCIAL = 150;
const LONGITUD_TELEFONO = 30;
const LONGITUD_EMAIL = 150;
const PATRON_NIT = /^\d{5,15}$/;

/** Registro o edicion de un proveedor. */
@Component({
  selector: 'app-dialogo-proveedor',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>{{ proveedor ? 'Editar proveedor' : 'Nuevo proveedor' }}</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <div class="formulario">
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Razón social o nombre</mat-label>
            <input matInput formControlName="razonSocial" />
            <mat-error>Ingrese la razón social</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>NIT (opcional)</mat-label>
            <input matInput formControlName="nit" inputmode="numeric" />
            <mat-error>Entre 5 y 15 dígitos</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Teléfono (opcional)</mat-label>
            <input matInput formControlName="telefono" inputmode="tel" />
          </mat-form-field>
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Correo (opcional)</mat-label>
            <input matInput type="email" formControlName="email" />
            <mat-error>Correo no válido</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Servicios que ofrece</mat-label>
            <textarea matInput rows="3" formControlName="servicios" placeholder="Transporte de carga, grúa, generador eléctrico…"></textarea>
            <mat-hint>Separe los servicios con comas o en líneas distintas.</mat-hint>
          </mat-form-field>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="guardando()">Guardar</button>
      </mat-dialog-actions>
    </form>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoProveedor {
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly gestion = inject(GestionProveedores);
  private readonly dialogo = inject(MatDialogRef<DialogoProveedor, Proveedor>);
  protected readonly proveedor = inject<Proveedor | undefined>(MAT_DIALOG_DATA);
  protected readonly guardando = signal(false);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    razonSocial: [this.proveedor?.razonSocial ?? '', [Validators.required, Validators.maxLength(LONGITUD_RAZON_SOCIAL)]],
    nit: [this.proveedor?.nit ?? '', Validators.pattern(PATRON_NIT)],
    telefono: [this.proveedor?.telefono ?? '', Validators.maxLength(LONGITUD_TELEFONO)],
    email: [this.proveedor?.email ?? '', [Validators.email, Validators.maxLength(LONGITUD_EMAIL)]],
    servicios: [this.proveedor?.servicios.join(', ') ?? ''],
  });

  protected guardar(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valores = this.formulario.getRawValue();
    guardarYCerrar(
      this.gestion.guardar(this.proveedor?.id ?? null, { ...valores, servicios: separarServicios(valores.servicios) }),
      this.dialogo,
      this.guardando,
      (error) => this.notificaciones.error(error),
    );
  }
}
