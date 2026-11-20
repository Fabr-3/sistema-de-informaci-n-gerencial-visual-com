import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

import { guardarYCerrar } from '../../../../../compartido/utilidades/guardar-y-cerrar';
import {
  CARNET_IDENTIDAD,
  ayudaDocumento,
  documentoValido,
  esCarnet,
  normalizarNumeroDocumento,
  soloLetras,
} from '../../../../../compartido/utilidades/validadores-identidad';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionPersonal } from '../../../aplicacion/gestion-personal';
import { CatalogosLogistica, DatosPersonal, PersonalTecnico } from '../../../dominio/modelos/personal';

const LONGITUD_NOMBRE = 100;
const LONGITUD_TELEFONO = 30;
const LONGITUD_DOCUMENTO = 50;
const LONGITUD_COMPLEMENTO = 2;
const TIPO_DOCUMENTO_POR_DEFECTO = CARNET_IDENTIDAD;

export interface DatosDialogoPersonal {
  readonly catalogos: CatalogosLogistica;
  readonly personal?: PersonalTecnico;
}

/** Registro o edicion del personal tecnico, con su cuenta del sistema opcional. */
@Component({
  selector: 'app-dialogo-personal',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ datos.personal ? 'Editar personal' : 'Nuevo personal técnico' }}</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <div class="formulario">
          <mat-form-field appearance="outline">
            <mat-label>Nombres</mat-label>
            <input matInput formControlName="nombres" />
            <mat-error>{{ formulario.controls.nombres.hasError('soloLetras') ? 'Solo letras' : 'Ingrese los nombres' }}</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Apellidos</mat-label>
            <input matInput formControlName="apellidos" />
            <mat-error>{{ formulario.controls.apellidos.hasError('soloLetras') ? 'Solo letras' : 'Ingrese los apellidos' }}</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Documento</mat-label>
            <mat-select formControlName="idTipoDocumento">
              @for (tipo of datos.catalogos.tiposDocumento; track tipo.id) {
                <mat-option [value]="tipo.id">{{ tipo.nombre }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Número</mat-label>
            <input matInput formControlName="numeroDocumento" placeholder="4876512 LP" />
            <mat-error>Ingrese el número de documento</mat-error>
            @if (formulario.hasError('numeroDocumento')) {
              <mat-hint class="error-grupo">{{ ayudaDocumento() }}</mat-hint>
            }
          </mat-form-field>
          @if (esCarnet()) {
            <mat-form-field appearance="outline">
              <mat-label>Complemento (opcional)</mat-label>
              <input matInput formControlName="complementoDocumento" maxlength="2" placeholder="1A" />
              @if (formulario.hasError('complemento')) {
                <mat-hint class="error-grupo">1 o 2 letras o números</mat-hint>
              }
            </mat-form-field>
          }
          <mat-form-field appearance="outline">
            <mat-label>Teléfono (opcional)</mat-label>
            <input matInput formControlName="telefono" inputmode="tel" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Especialidades</mat-label>
            <mat-select formControlName="idsEspecialidades" multiple>
              @for (especialidad of datos.catalogos.especialidades; track especialidad.id) {
                <mat-option [value]="especialidad.id">{{ especialidad.nombre }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Cuenta del sistema (opcional)</mat-label>
            <mat-select formControlName="idUsuario">
              <mat-option [value]="null">Sin cuenta</mat-option>
              @for (usuario of usuariosDisponibles(); track usuario.id) {
                <mat-option [value]="usuario.id">{{ usuario.nombreCompleto }} ({{ usuario.nombreUsuario }})</mat-option>
              }
            </mat-select>
            <mat-hint>Le permite consultar sus asignaciones en «Mis asignaciones».</mat-hint>
          </mat-form-field>
          <mat-slide-toggle formControlName="activo" class="completo interruptor">Activo (se le puede asignar trabajo)</mat-slide-toggle>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="guardando()">Guardar</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .interruptor {
      margin: 12px 0 4px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoPersonal {
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly gestion = inject(GestionPersonal);
  private readonly dialogo = inject(MatDialogRef<DialogoPersonal, PersonalTecnico>);
  protected readonly datos = inject<DatosDialogoPersonal>(MAT_DIALOG_DATA);
  protected readonly guardando = signal(false);

  /** Cuentas sin vincular mas la que ya tiene la persona editada. */
  protected readonly usuariosDisponibles = computed(() => {
    const actual = this.datos.personal?.usuario;
    return actual ? [actual, ...this.datos.catalogos.usuarios] : this.datos.catalogos.usuarios;
  });

  protected readonly formulario = inject(NonNullableFormBuilder).group(
    {
      nombres: [this.datos.personal?.nombres ?? '', [Validators.required, Validators.maxLength(LONGITUD_NOMBRE), soloLetras]],
      apellidos: [
        this.datos.personal?.apellidos ?? '',
        [Validators.required, Validators.maxLength(LONGITUD_NOMBRE), soloLetras],
      ],
      idTipoDocumento: [this.datos.personal?.documento.tipo.id ?? this.tipoPorDefecto(), Validators.min(1)],
      numeroDocumento: [
        this.datos.personal?.documento.numero ?? '',
        [Validators.required, Validators.maxLength(LONGITUD_DOCUMENTO)],
      ],
      complementoDocumento: [this.datos.personal?.documento.complemento ?? '', Validators.maxLength(LONGITUD_COMPLEMENTO)],
      telefono: [this.datos.personal?.telefono ?? '', Validators.maxLength(LONGITUD_TELEFONO)],
      idsEspecialidades: [this.datos.personal?.especialidades.map((especialidad) => especialidad.id) ?? []],
      idUsuario: [this.datos.personal?.usuario?.id ?? (null as number | null)],
      activo: [this.datos.personal?.activo ?? true],
    },
    {
      validators: documentoValido((idTipo) => this.nombreTipoDocumento(idTipo), {
        tipo: 'idTipoDocumento',
        numero: 'numeroDocumento',
        complemento: 'complementoDocumento',
      }),
    },
  );
  private readonly idTipoDocumento = toSignal(this.formulario.controls.idTipoDocumento.valueChanges, {
    initialValue: this.formulario.controls.idTipoDocumento.value,
  });
  protected readonly esCarnet = computed(() => esCarnet(this.nombreTipoDocumento(this.idTipoDocumento())));
  protected readonly ayudaDocumento = computed(() => ayudaDocumento(this.nombreTipoDocumento(this.idTipoDocumento())));

  protected guardar(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    guardarYCerrar(
      this.gestion.guardar(this.datos.personal?.id ?? null, this.datosParaGuardar()),
      this.dialogo,
      this.guardando,
      (error) => this.notificaciones.error(error),
    );
  }

  private datosParaGuardar(): DatosPersonal {
    const valores = this.formulario.getRawValue();
    return {
      ...valores,
      numeroDocumento: normalizarNumeroDocumento(valores.numeroDocumento),
      complementoDocumento: this.esCarnet() ? valores.complementoDocumento.trim().toUpperCase() || null : null,
    };
  }

  private nombreTipoDocumento(idTipo: number): string | undefined {
    return this.datos.catalogos.tiposDocumento.find((tipo) => tipo.id === idTipo)?.nombre;
  }

  private tipoPorDefecto(): number {
    return this.datos.catalogos.tiposDocumento.find((tipo) => tipo.nombre === TIPO_DOCUMENTO_POR_DEFECTO)?.id ?? 0;
  }
}
