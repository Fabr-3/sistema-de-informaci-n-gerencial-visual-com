import { KeyValuePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { CatalogosActivos } from '../../../aplicacion/catalogos-activos';
import { Categoria, Fabricante } from '../../../dominio/modelos/catalogos';
import { Especificacion, ModeloActivo, TIPOS_CONTROL, TipoControl } from '../../../dominio/modelos/modelo-activo';

export interface DatosDialogoModelo {
  readonly modelo?: ModeloActivo;
  readonly fabricantes: readonly Fabricante[];
  readonly categorias: readonly Categoria[];
}

const LONGITUD_NOMBRE = 100;
const LONGITUD_CARACTERISTICA = 100;
const LONGITUD_VALOR = 150;
const MAXIMO_ESPECIFICACIONES = 30;
const FORMATO_PREFIJO = /^[A-Za-z0-9]{2,10}$/;

/**
 * Alta y edicion de modelos con sus especificaciones. El intervalo de
 * mantenimiento por horas solo aplica a modelos controlados por unidad.
 */
@Component({
  selector: 'app-dialogo-modelo',
  imports: [
    KeyValuePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './dialogo-modelo.html',
  styleUrl: './dialogo-modelo.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoModelo {
  protected readonly datos = inject<DatosDialogoModelo>(MAT_DIALOG_DATA);
  private readonly dialogo = inject(MatDialogRef<DialogoModelo, ModeloActivo>);
  private readonly catalogos = inject(CatalogosActivos);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly formularios = inject(NonNullableFormBuilder);

  protected readonly tiposControl = TIPOS_CONTROL;
  protected readonly ordenOriginal = (): number => 0;
  protected readonly maximoEspecificaciones = MAXIMO_ESPECIFICACIONES;
  protected readonly guardando = signal(false);
  private readonly modelo = this.datos.modelo;

  protected readonly formulario = this.formularios.group({
    idFabricante: [this.modelo?.fabricante.id ?? 0, Validators.min(1)],
    idCategoria: [this.modelo?.categoria.id ?? 0, Validators.min(1)],
    nombre: [this.modelo?.nombre ?? '', [Validators.required, Validators.maxLength(LONGITUD_NOMBRE)]],
    tipoControl: [this.modelo?.tipoControl ?? ('UNIDAD' as TipoControl), Validators.required],
    horasMantenimiento: [this.modelo?.horasMantenimiento ?? (null as number | null), Validators.min(1)],
    prefijoCodigo: [this.modelo?.prefijoCodigo ?? '', [Validators.required, Validators.pattern(FORMATO_PREFIJO)]],
    especificaciones: this.formularios.array(
      (this.modelo?.especificaciones ?? []).map((especificacion) => this.grupoEspecificacion(especificacion)),
    ),
  });

  constructor() {
    this.formulario.controls.tipoControl.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((tipo) => this.ajustarSegunTipo(tipo, true));
    this.ajustarSegunTipo(this.formulario.controls.tipoControl.value, false);
  }

  protected get especificaciones() {
    return this.formulario.controls.especificaciones;
  }

  protected agregarEspecificacion(): void {
    this.especificaciones.push(this.grupoEspecificacion());
  }

  protected quitarEspecificacion(indice: number): void {
    this.especificaciones.removeAt(indice);
  }

  protected guardar(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valores = this.formulario.getRawValue();
    this.guardando.set(true);
    this.catalogos
      .guardarModelo(this.modelo?.id ?? null, {
        idFabricante: valores.idFabricante,
        idCategoria: valores.idCategoria,
        nombre: valores.nombre,
        tipoControl: valores.tipoControl,
        horasMantenimiento: valores.horasMantenimiento,
        prefijoCodigo: valores.prefijoCodigo,
        especificaciones: valores.especificaciones.map((especificacion) => ({
          caracteristica: especificacion.caracteristica.trim(),
          valor: especificacion.valor.trim(),
        })),
      })
      .subscribe({
        next: (modelo) => this.dialogo.close(modelo),
        error: (error: unknown) => {
          this.notificaciones.error(error);
          this.guardando.set(false);
        },
      });
  }

  /** Horas de mantenimiento y prefijo solo aplican a modelos controlados por unidad. */
  private ajustarSegunTipo(tipo: TipoControl, limpiar: boolean): void {
    const { horasMantenimiento, prefijoCodigo } = this.formulario.controls;
    if (tipo === 'LOTE') {
      if (limpiar) {
        horasMantenimiento.reset(null);
        prefijoCodigo.reset('');
      }
      horasMantenimiento.disable();
      prefijoCodigo.disable();
    } else {
      horasMantenimiento.enable();
      prefijoCodigo.enable();
    }
  }

  private grupoEspecificacion(especificacion?: Especificacion) {
    return this.formularios.group({
      caracteristica: [
        especificacion?.caracteristica ?? '',
        [Validators.required, Validators.maxLength(LONGITUD_CARACTERISTICA)],
      ],
      valor: [especificacion?.valor ?? '', [Validators.required, Validators.maxLength(LONGITUD_VALOR)]],
    });
  }
}
