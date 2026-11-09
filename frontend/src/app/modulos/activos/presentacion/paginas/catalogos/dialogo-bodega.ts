import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { CatalogosActivos } from '../../../aplicacion/catalogos-activos';
import { Bodega } from '../../../dominio/modelos/catalogos';

const LONGITUD_NOMBRE = 100;
const LONGITUD_DIRECCION = 500;

/** Alta y edicion de bodegas. Una bodega inactiva no recibe activos nuevos. */
@Component({
  selector: 'app-dialogo-bodega',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ bodega ? 'Editar bodega' : 'Nueva bodega' }}</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <div class="formulario">
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Nombre</mat-label>
            <input matInput formControlName="nombre" />
            <mat-error>Ingrese el nombre</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Dirección (opcional)</mat-label>
            <input matInput formControlName="direccion" />
          </mat-form-field>
          <mat-slide-toggle formControlName="activa" class="completo">Bodega activa</mat-slide-toggle>
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
export class DialogoBodega {
  protected readonly bodega = inject<Bodega | null>(MAT_DIALOG_DATA);
  private readonly dialogo = inject(MatDialogRef<DialogoBodega, Bodega>);
  private readonly catalogos = inject(CatalogosActivos);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly guardando = signal(false);
  protected readonly formulario = inject(NonNullableFormBuilder).group({
    nombre: [this.bodega?.nombre ?? '', [Validators.required, Validators.maxLength(LONGITUD_NOMBRE)]],
    direccion: [this.bodega?.direccion ?? '', Validators.maxLength(LONGITUD_DIRECCION)],
    activa: [this.bodega?.activa ?? true],
  });

  protected guardar(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const { nombre, direccion, activa } = this.formulario.getRawValue();
    this.guardando.set(true);
    this.catalogos.guardarBodega(this.bodega?.id ?? null, { nombre, direccion: direccion.trim() || null, activa }).subscribe({
      next: (bodega) => this.dialogo.close(bodega),
      error: (error: unknown) => {
        this.notificaciones.error(error);
        this.guardando.set(false);
      },
    });
  }
}
