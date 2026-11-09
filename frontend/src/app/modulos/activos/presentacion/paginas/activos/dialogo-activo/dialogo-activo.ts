import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Observable, map } from 'rxjs';

import { aFechaIso } from '../../../../../../compartido/utilidades/fechas';
import { ServicioNotificaciones } from '../../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionActivos } from '../../../../aplicacion/gestion-activos';
import { Activo, CANTIDAD_MAXIMA_REGISTRO } from '../../../../dominio/modelos/activo';
import { Bodega } from '../../../../dominio/modelos/catalogos';
import { ModeloActivo, nombreCompletoModelo } from '../../../../dominio/modelos/modelo-activo';

export interface DatosDialogoActivo {
  /** Ausente al registrar unidades nuevas. */
  readonly activo?: Activo;
  /** Solo modelos controlados por unidad. */
  readonly modelos: readonly ModeloActivo[];
  readonly bodegas: readonly Bodega[];
  readonly idModelo?: number;
}

const LONGITUD_CODIGO = 50;
const LONGITUD_SERIE = 100;
const LONGITUD_OBSERVACIONES = 2000;
const SEPARADOR_SERIES = /\r?\n/;

/**
 * Alta de una o varias unidades de un modelo (cada una con su codigo
 * correlativo) y edicion de un activo existente.
 */
@Component({
  selector: 'app-dialogo-activo',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './dialogo-activo.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoActivo {
  private readonly datos = inject<DatosDialogoActivo>(MAT_DIALOG_DATA);
  /** Se cierra con los activos registrados o con el activo editado. */
  private readonly dialogo = inject(MatDialogRef<DialogoActivo, Activo[]>);
  private readonly gestion = inject(GestionActivos);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly activo = this.datos.activo;
  protected readonly modelos = this.datos.modelos;
  protected readonly nombreModelo = nombreCompletoModelo;
  protected readonly cantidadMaxima = CANTIDAD_MAXIMA_REGISTRO;
  protected readonly hoy = new Date();
  protected readonly guardando = signal(false);
  /** Bodegas activas mas la actual del activo (aunque se haya desactivado). */
  protected readonly bodegas = this.datos.bodegas.filter(
    (bodega) => bodega.activa || bodega.id === this.datos.activo?.idBodega,
  );

  protected readonly formulario = inject(NonNullableFormBuilder).group(
    {
      idModelo: [this.activo?.idModelo ?? this.datos.idModelo ?? 0, Validators.min(1)],
      idBodega: [this.activo?.idBodega ?? 0, Validators.min(1)],
      cantidad: [1, [Validators.required, Validators.min(1), Validators.max(CANTIDAD_MAXIMA_REGISTRO)]],
      codigoInventario: ['', Validators.maxLength(LONGITUD_CODIGO)],
      numerosSerie: [''],
      numeroSerie: [this.activo?.numeroSerie ?? '', Validators.maxLength(LONGITUD_SERIE)],
      fechaAdquisicion: [
        this.activo?.fechaAdquisicion ? new Date(`${this.activo.fechaAdquisicion}T00:00`) : (null as Date | null),
      ],
      costo: [this.activo?.costoAdquisicion ?? (null as number | null), Validators.min(0)],
      observaciones: [this.activo?.observaciones ?? '', Validators.maxLength(LONGITUD_OBSERVACIONES)],
    },
    { validators: seriesSegunCantidad },
  );

  private readonly valores = toSignal(this.formulario.valueChanges, { initialValue: this.formulario.getRawValue() });
  protected readonly cantidad = computed(() => Number(this.valores().cantidad) || 1);
  protected readonly modeloSeleccionado = computed(() =>
    this.modelos.find((modelo) => modelo.id === this.valores().idModelo),
  );

  protected guardar(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.operacion().subscribe({
      next: (activos) => this.dialogo.close(activos),
      error: (error: unknown) => {
        this.notificaciones.error(error);
        this.guardando.set(false);
      },
    });
  }

  private operacion(): Observable<Activo[]> {
    const valores = this.formulario.getRawValue();
    const fechaAdquisicion = valores.fechaAdquisicion ? aFechaIso(valores.fechaAdquisicion) : null;
    const observaciones = valores.observaciones.trim() || null;
    if (this.activo) {
      return this.gestion
        .actualizar(this.activo.id, {
          numeroSerie: valores.numeroSerie.trim() || null,
          idModelo: valores.idModelo,
          idBodega: valores.idBodega,
          fechaAdquisicion,
          costoAdquisicion: valores.costo,
          observaciones,
        })
        .pipe(map((activo) => [activo]));
    }
    return this.gestion.registrar({
      idModelo: valores.idModelo,
      idBodega: valores.idBodega,
      cantidad: Number(valores.cantidad),
      codigoInventario: Number(valores.cantidad) === 1 ? valores.codigoInventario : null,
      numerosSerie: valores.numerosSerie.split(SEPARADOR_SERIES),
      fechaAdquisicion,
      costoUnitario: valores.costo,
      observaciones,
    });
  }
}

/** Las series son opcionales; si se indican, debe haber una por unidad. */
const seriesSegunCantidad: ValidatorFn = (grupo: AbstractControl): ValidationErrors | null => {
  const series: string[] = (grupo.get('numerosSerie')?.value ?? '')
    .split(SEPARADOR_SERIES)
    .map((serie: string) => serie.trim())
    .filter((serie: string) => serie.length > 0);
  const cantidad = Number(grupo.get('cantidad')?.value) || 0;
  return series.length === 0 || series.length === cantidad ? null : { seriesIncompletas: true };
};
