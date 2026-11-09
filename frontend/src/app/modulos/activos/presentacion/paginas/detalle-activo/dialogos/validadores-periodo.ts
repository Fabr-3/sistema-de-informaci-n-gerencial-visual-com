import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Valida en el grupo que el campo de fin sea posterior al de inicio (valores datetime-local). */
export function finPosteriorAInicio(campoInicio: string, campoFin: string): ValidatorFn {
  return (grupo: AbstractControl): ValidationErrors | null => {
    const inicio: string = grupo.get(campoInicio)?.value ?? '';
    const fin: string = grupo.get(campoFin)?.value ?? '';
    return inicio && fin && fin <= inicio ? { periodoInvalido: true } : null;
  };
}

/** La fecha y hora (datetime-local) no puede ser posterior al momento actual. */
export const noFutura: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const valor: string = control.value ?? '';
  return valor && new Date(valor) > new Date() ? { fechaFutura: true } : null;
};
