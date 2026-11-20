import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import {
  EntradaCalculadoraLed,
  MODULOS_HABITUALES,
  ModoMedida,
  PITCHES_HABITUALES,
  calcularPantallaLed,
} from '../../dominio/calculadora-led';

const PITCH_INICIAL = 3.9;
const DIAS_INICIALES = 1;

/** Calculadora de pantallas LED: consulta rapida para cotizar, sin guardar datos. */
@Component({
  selector: 'app-pagina-calculadora-led',
  imports: [
    CurrencyPipe,
    DecimalPipe,
    ReactiveFormsModule,
    MatButtonToggleModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './pagina-calculadora-led.html',
  styleUrl: './pagina-calculadora-led.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaCalculadoraLed {
  protected readonly pitches = PITCHES_HABITUALES;
  protected readonly modulosHabituales = MODULOS_HABITUALES;
  protected readonly formulario = inject(NonNullableFormBuilder).group({
    modo: ['dimensiones' as ModoMedida],
    ancho: [null as number | null, Validators.min(0.01)],
    alto: [null as number | null, Validators.min(0.01)],
    area: [null as number | null, Validators.min(0.01)],
    pitchMm: [PITCH_INICIAL, [Validators.required, Validators.min(0.1)]],
    modulo: [0],
    precioM2Dia: [null as number | null, Validators.min(0)],
    dias: [DIAS_INICIALES, [Validators.required, Validators.min(1)]],
  });

  private readonly valores = toSignal(this.formulario.valueChanges, { initialValue: this.formulario.getRawValue() });
  protected readonly modo = computed(() => this.valores().modo ?? 'dimensiones');
  protected readonly resultado = computed(() => {
    const valores = { ...this.formulario.getRawValue(), ...this.valores() };
    const modulo = MODULOS_HABITUALES[valores.modulo] ?? MODULOS_HABITUALES[0];
    const entrada: EntradaCalculadoraLed = {
      modo: valores.modo,
      ancho: numero(valores.ancho),
      alto: numero(valores.alto),
      area: numero(valores.area),
      pitchMm: numero(valores.pitchMm) ?? 0,
      moduloAnchoMm: modulo.ancho,
      moduloAltoMm: modulo.alto,
      precioM2Dia: numero(valores.precioM2Dia) ?? 0,
      dias: numero(valores.dias) ?? 0,
    };
    return calcularPantallaLed(entrada);
  });

  protected elegirPitch(pitch: number): void {
    this.formulario.controls.pitchMm.setValue(pitch);
  }
}

/** Los campos numericos vacios llegan como null o texto vacio. */
function numero(valor: number | string | null | undefined): number | null {
  if (valor === null || valor === undefined || valor === '') {
    return null;
  }
  const convertido = Number(valor);
  return Number.isFinite(convertido) ? convertido : null;
}
