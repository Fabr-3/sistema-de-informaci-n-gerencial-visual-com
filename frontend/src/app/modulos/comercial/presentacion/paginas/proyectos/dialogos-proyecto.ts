import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { aFechaIso, desdeFechaIso } from '../../../../../compartido/utilidades/fechas';
import { guardarYCerrar } from '../../../../../compartido/utilidades/guardar-y-cerrar';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { finPosteriorAInicio } from '../../../../../compartido/utilidades/validadores-periodo';
import { GestionProyectos } from '../../../aplicacion/gestion-proyectos';
import { Lugar, etiquetaLugar } from '../../../dominio/modelos/catalogos';
import { Contrato, Evento, Proyecto } from '../../../dominio/modelos/proyecto';

const LONGITUD_NOMBRE = 200;
const LONGITUD_DESCRIPCION = 2000;
const LONGITUD_TERMINOS = 10_000;

/** Datos y periodo del proyecto (el periodo debe contener sus eventos). */
@Component({
  selector: 'app-dialogo-proyecto',
  imports: [ReactiveFormsModule, MatButtonModule, MatDatepickerModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>Editar proyecto</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <div class="formulario">
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Nombre</mat-label>
            <input matInput formControlName="nombre" />
            <mat-error>Ingrese el nombre</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Inicio</mat-label>
            <input matInput formControlName="fechaInicio" [matDatepicker]="inicio" />
            <mat-datepicker-toggle matIconSuffix [for]="inicio" />
            <mat-datepicker #inicio />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Fin</mat-label>
            <input matInput formControlName="fechaFin" [matDatepicker]="fin" />
            <mat-datepicker-toggle matIconSuffix [for]="fin" />
            <mat-datepicker #fin />
          </mat-form-field>
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Descripción (opcional)</mat-label>
            <textarea matInput rows="3" formControlName="descripcion"></textarea>
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
export class DialogoProyecto {
  private readonly notificaciones = inject(ServicioNotificaciones);
  protected readonly guardando = signal(false);
  private readonly proyecto = inject<Proyecto>(MAT_DIALOG_DATA);
  private readonly dialogo = inject(MatDialogRef<DialogoProyecto, Proyecto>);
  private readonly gestion = inject(GestionProyectos);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    nombre: [this.proyecto.nombre, [Validators.required, Validators.maxLength(LONGITUD_NOMBRE)]],
    descripcion: [this.proyecto.descripcion ?? '', Validators.maxLength(LONGITUD_DESCRIPCION)],
    fechaInicio: [desdeFechaIso(this.proyecto.fechaInicio)],
    fechaFin: [desdeFechaIso(this.proyecto.fechaFin)],
  });

  protected guardar(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valores = this.formulario.getRawValue();
    guardarYCerrar(
      this.gestion.actualizar(this.proyecto.id, {
        nombre: valores.nombre,
        descripcion: valores.descripcion,
        fechaInicio: valores.fechaInicio ? aFechaIso(valores.fechaInicio) : null,
        fechaFin: valores.fechaFin ? aFechaIso(valores.fechaFin) : null,
      }),
      this.dialogo,
      this.guardando,
      (error) => this.notificaciones.error(error),
    );
  }
}

export interface DatosDialogoEvento {
  readonly idProyecto: number;
  readonly evento?: Evento;
  readonly lugares: readonly Lugar[];
}

/** Alta y edicion de un evento del cronograma del proyecto. */
@Component({
  selector: 'app-dialogo-evento',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>{{ datos.evento ? 'Editar evento' : 'Programar evento' }}</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <div class="formulario">
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Nombre</mat-label>
            <input matInput formControlName="nombre" placeholder="Montaje, función, desmontaje…" />
            <mat-error>Ingrese el nombre</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Lugar</mat-label>
            <mat-select formControlName="idLugar">
              @for (lugar of datos.lugares; track lugar.id) {
                <mat-option [value]="lugar.id">{{ etiquetaLugar(lugar) }}</mat-option>
              }
            </mat-select>
            <mat-error>Seleccione el lugar</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Inicio</mat-label>
            <input matInput type="datetime-local" formControlName="fechaInicio" />
            <mat-error>Indique el inicio</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Fin</mat-label>
            <input matInput type="datetime-local" formControlName="fechaFin" />
            <mat-error>Indique el fin</mat-error>
          </mat-form-field>
          @if (formulario.hasError('periodoInvalido')) {
            <p class="completo error-grupo" role="alert">El fin debe ser posterior al inicio</p>
          }
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
export class DialogoEvento {
  private readonly notificaciones = inject(ServicioNotificaciones);
  protected readonly guardando = signal(false);
  protected readonly datos = inject<DatosDialogoEvento>(MAT_DIALOG_DATA);
  private readonly dialogo = inject(MatDialogRef<DialogoEvento, Evento>);
  private readonly gestion = inject(GestionProyectos);
  protected readonly etiquetaLugar = etiquetaLugar;

  protected readonly formulario = inject(NonNullableFormBuilder).group(
    {
      nombre: [this.datos.evento?.nombre ?? '', [Validators.required, Validators.maxLength(LONGITUD_NOMBRE)]],
      idLugar: [this.datos.evento?.lugar.id ?? 0, Validators.min(1)],
      fechaInicio: [this.datos.evento?.fechaInicio.substring(0, 16) ?? '', Validators.required],
      fechaFin: [this.datos.evento?.fechaFin.substring(0, 16) ?? '', Validators.required],
    },
    { validators: finPosteriorAInicio('fechaInicio', 'fechaFin') },
  );

  protected guardar(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    guardarYCerrar(
      this.gestion.guardarEvento(this.datos.idProyecto, this.datos.evento?.id ?? null, this.formulario.getRawValue()),
      this.dialogo,
      this.guardando,
      (error) => this.notificaciones.error(error),
    );
  }
}

export interface DatosDialogoContrato {
  readonly idProyecto: number;
  readonly contrato?: Contrato;
}

/** Registro del contrato firmado; el PDF se genera con la plantilla estandar. */
@Component({
  selector: 'app-dialogo-contrato',
  imports: [ReactiveFormsModule, MatButtonModule, MatDatepickerModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>{{ datos.contrato ? 'Editar ' + datos.contrato.numero : 'Registrar contrato' }}</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <p class="indicacion">
          El documento usa la plantilla estándar con los datos del proyecto, la cotización y el cronograma. Los
          términos particulares se agregan a continuación de las cláusulas.
        </p>
        <mat-form-field appearance="outline" class="campo">
          <mat-label>Fecha de firma</mat-label>
          <input matInput formControlName="fechaFirma" [matDatepicker]="firma" [max]="hoy" />
          <mat-datepicker-toggle matIconSuffix [for]="firma" />
          <mat-datepicker #firma />
          <mat-error>Indique una fecha que no sea futura</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline" class="campo">
          <mat-label>Términos particulares (opcional)</mat-label>
          <textarea matInput rows="5" formControlName="terminos" placeholder="Forma de pago acordada, condiciones especiales…"></textarea>
        </mat-form-field>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="guardando()">Guardar</button>
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
export class DialogoContrato {
  private readonly notificaciones = inject(ServicioNotificaciones);
  protected readonly guardando = signal(false);
  protected readonly datos = inject<DatosDialogoContrato>(MAT_DIALOG_DATA);
  private readonly dialogo = inject(MatDialogRef<DialogoContrato, Contrato>);
  private readonly gestion = inject(GestionProyectos);
  protected readonly hoy = new Date();

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    fechaFirma: [desdeFechaIso(this.datos.contrato?.fechaFirma ?? null) ?? new Date(), Validators.required],
    terminos: [this.datos.contrato?.terminos ?? '', Validators.maxLength(LONGITUD_TERMINOS)],
  });

  protected guardar(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const { fechaFirma, terminos } = this.formulario.getRawValue();
    guardarYCerrar(
      this.gestion.guardarContrato(this.datos.idProyecto, this.datos.contrato?.id ?? null, {
        fechaFirma: aFechaIso(fechaFirma!),
        terminos,
      }),
      this.dialogo,
      this.guardando,
      (error) => this.notificaciones.error(error),
    );
  }
}

