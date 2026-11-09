import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

import { guardarYCerrar } from '../../../../../compartido/utilidades/guardar-y-cerrar';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionComercial } from '../../../aplicacion/gestion-comercial';
import { Lugar, Servicio } from '../../../dominio/modelos/catalogos';

const LONGITUD_NOMBRE_SERVICIO = 100;
const LONGITUD_DESCRIPCION = 1000;
const LONGITUD_UNIDAD = 30;
const LONGITUD_NOMBRE_LUGAR = 150;
const LONGITUD_DIRECCION = 500;
const LONGITUD_CIUDAD = 80;

/** Servicio ofrecido con su unidad de medida y precio referencial en Bs. */
@Component({
  selector: 'app-dialogo-servicio',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSlideToggleModule],
  template: `
    <h2 mat-dialog-title>{{ servicio ? 'Editar servicio' : 'Nuevo servicio' }}</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <div class="formulario">
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Nombre</mat-label>
            <input matInput formControlName="nombre" />
            <mat-error>Ingrese el nombre</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Unidad de medida</mat-label>
            <input matInput formControlName="unidadMedida" placeholder="m² por día, jornada, viaje…" />
            <mat-error>Ingrese la unidad</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Precio referencial en Bs</mat-label>
            <input matInput type="number" min="0" step="0.01" formControlName="precioReferencial" />
            <mat-error>Ingrese un precio válido</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Descripción (opcional)</mat-label>
            <textarea matInput rows="2" formControlName="descripcion"></textarea>
          </mat-form-field>
          <mat-slide-toggle formControlName="activo" class="completo">Disponible para nuevas cotizaciones</mat-slide-toggle>
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
export class DialogoServicio {
  protected readonly servicio = inject<Servicio | null>(MAT_DIALOG_DATA);
  private readonly dialogo = inject(MatDialogRef<DialogoServicio, Servicio>);
  private readonly gestion = inject(GestionComercial);
  private readonly notificaciones = inject(ServicioNotificaciones);
  protected readonly guardando = signal(false);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    nombre: [this.servicio?.nombre ?? '', [Validators.required, Validators.maxLength(LONGITUD_NOMBRE_SERVICIO)]],
    unidadMedida: [this.servicio?.unidadMedida ?? '', [Validators.required, Validators.maxLength(LONGITUD_UNIDAD)]],
    precioReferencial: [this.servicio?.precioReferencial ?? (null as number | null), [Validators.required, Validators.min(0)]],
    descripcion: [this.servicio?.descripcion ?? '', Validators.maxLength(LONGITUD_DESCRIPCION)],
    activo: [this.servicio?.activo ?? true],
  });

  protected guardar(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valores = this.formulario.getRawValue();
    guardarYCerrar(
      this.gestion.guardarServicio(this.servicio?.id ?? null, { ...valores, precioReferencial: Number(valores.precioReferencial) }),
      this.dialogo,
      this.guardando,
      (error) => this.notificaciones.error(error),
    );
  }
}

/** Lugar donde se realizan los eventos. */
@Component({
  selector: 'app-dialogo-lugar',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>{{ lugar ? 'Editar lugar' : 'Nuevo lugar' }}</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <div class="formulario">
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Nombre</mat-label>
            <input matInput formControlName="nombre" placeholder="Hotel, coliseo, recinto ferial…" />
            <mat-error>Ingrese el nombre</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Dirección</mat-label>
            <input matInput formControlName="direccion" />
            <mat-error>Ingrese la dirección</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Ciudad (opcional)</mat-label>
            <input matInput formControlName="ciudad" />
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
export class DialogoLugar {
  protected readonly lugar = inject<Lugar | null>(MAT_DIALOG_DATA);
  private readonly dialogo = inject(MatDialogRef<DialogoLugar, Lugar>);
  private readonly gestion = inject(GestionComercial);
  private readonly notificaciones = inject(ServicioNotificaciones);
  protected readonly guardando = signal(false);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    nombre: [this.lugar?.nombre ?? '', [Validators.required, Validators.maxLength(LONGITUD_NOMBRE_LUGAR)]],
    direccion: [this.lugar?.direccion ?? '', [Validators.required, Validators.maxLength(LONGITUD_DIRECCION)]],
    ciudad: [this.lugar?.ciudad ?? '', Validators.maxLength(LONGITUD_CIUDAD)],
  });

  protected guardar(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    guardarYCerrar(
      this.gestion.guardarLugar(this.lugar?.id ?? null, this.formulario.getRawValue()),
      this.dialogo,
      this.guardando,
      (error) => this.notificaciones.error(error),
    );
  }
}
