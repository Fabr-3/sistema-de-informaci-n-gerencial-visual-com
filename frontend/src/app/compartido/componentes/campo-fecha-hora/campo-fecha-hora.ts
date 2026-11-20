import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  ControlValueAccessor,
  FormControl,
  FormGroupDirective,
  NgControl,
  NgForm,
  ReactiveFormsModule,
} from '@angular/forms';
import { ErrorStateMatcher } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTimepickerModule } from '@angular/material/timepicker';
import { merge } from 'rxjs';

import { combinarFechaHora, separarFechaHora } from './fecha-hora';

/**
 * Fecha y hora con calendario desplegable y lista de horas, en lugar de
 * escribirlas. El valor es texto "AAAA-MM-DDTHH:mm" (vacio si falta algo),
 * igual que un campo datetime-local.
 */
@Component({
  selector: 'app-campo-fecha-hora',
  imports: [ReactiveFormsModule, MatDatepickerModule, MatFormFieldModule, MatInputModule, MatTimepickerModule],
  template: `
    <mat-form-field appearance="outline" class="fecha">
      <mat-label>{{ etiqueta() }}</mat-label>
      <input
        matInput
        [matDatepicker]="calendario"
        [formControl]="fecha"
        [min]="minimo()"
        [errorStateMatcher]="errores"
        placeholder="dd/mm/aaaa"
      />
      <mat-datepicker-toggle matIconSuffix [for]="calendario" />
      <mat-datepicker #calendario [startAt]="inicioCalendario()" />
      @if (mensajeError()) {
        <mat-error>{{ mensajeError() }}</mat-error>
      }
    </mat-form-field>
    <mat-form-field appearance="outline" class="hora">
      <mat-label>Hora</mat-label>
      <input
        matInput
        [matTimepicker]="reloj"
        [formControl]="hora"
        [errorStateMatcher]="errores"
        (blur)="tocar()"
        placeholder="hh:mm"
      />
      <mat-timepicker-toggle matIconSuffix [for]="reloj" />
      <mat-timepicker #reloj [interval]="intervalo()" (closed)="tocar()" />
    </mat-form-field>
  `,
  styles: `
    :host {
      display: grid;
      grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr);
      gap: 8px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CampoFechaHora implements ControlValueAccessor {
  readonly etiqueta = input('Fecha');
  /** Paso de la lista de horas (por ejemplo "30min" o "1h"). */
  readonly intervalo = input('30min');
  readonly minimo = input<Date | null>(null);
  /** Fecha "AAAA-MM-DD..." en la que se abre el calendario si el campo esta vacio (por ejemplo, la del evento). */
  readonly referencia = input<string | null>(null);
  protected readonly inicioCalendario = computed(() => separarFechaHora(this.referencia()).fecha);
  readonly mensajeError = input('Indique la fecha y la hora');

  private readonly control = inject(NgControl, { self: true, optional: true });
  protected readonly fecha = new FormControl<Date | null>(null);
  protected readonly hora = new FormControl<Date | null>(null);
  private alCambiar: (valor: string) => void = () => undefined;
  private alTocar: () => void = () => undefined;

  /** Los campos internos muestran el error del control externo (por ejemplo, requerido o fin antes del inicio). */
  protected readonly errores: ErrorStateMatcher = {
    isErrorState: (_control: unknown, _formulario: FormGroupDirective | NgForm | null) =>
      !!this.control?.invalid && !!this.control?.touched,
  };

  constructor() {
    if (this.control) {
      this.control.valueAccessor = this;
    }
    merge(this.fecha.valueChanges, this.hora.valueChanges)
      .pipe(takeUntilDestroyed(inject(DestroyRef)))
      .subscribe(() => this.alCambiar(combinarFechaHora(this.fecha.value, this.hora.value)));
  }

  writeValue(valor: string | null): void {
    const { fecha, hora } = separarFechaHora(valor);
    this.fecha.setValue(fecha, { emitEvent: false });
    this.hora.setValue(hora, { emitEvent: false });
  }

  registerOnChange(funcion: (valor: string) => void): void {
    this.alCambiar = funcion;
  }

  registerOnTouched(funcion: () => void): void {
    this.alTocar = funcion;
  }

  setDisabledState(deshabilitado: boolean): void {
    for (const control of [this.fecha, this.hora]) {
      if (deshabilitado) {
        control.disable({ emitEvent: false });
      } else {
        control.enable({ emitEvent: false });
      }
    }
  }

  protected tocar(): void {
    this.alTocar();
  }
}
