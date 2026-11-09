import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Debe coincidir con PoliticaContrasena del backend. */
export const LONGITUD_MINIMA_CONTRASENA = 8;
export const LONGITUD_MAXIMA_CONTRASENA = 72;
export const DESCRIPCION_POLITICA_CONTRASENA =
  'Entre 8 y 72 caracteres, con al menos una letra y un número.';

const CONTIENE_LETRA = /\p{L}/u;
const CONTIENE_NUMERO = /\d/;

export const politicaContrasena: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const valor: string = control.value ?? '';
  if (!valor) {
    return null;
  }
  const valida =
    valor.length >= LONGITUD_MINIMA_CONTRASENA &&
    valor.length <= LONGITUD_MAXIMA_CONTRASENA &&
    CONTIENE_LETRA.test(valor) &&
    CONTIENE_NUMERO.test(valor);
  return valida ? null : { politicaContrasena: true };
};

/** Valida en el grupo que dos campos tengan el mismo valor. */
export function camposIguales(campo: string, confirmacion: string): ValidatorFn {
  return (grupo: AbstractControl): ValidationErrors | null => {
    const valor = grupo.get(campo)?.value;
    const valorConfirmacion = grupo.get(confirmacion)?.value;
    return valorConfirmacion && valor !== valorConfirmacion ? { noCoinciden: true } : null;
  };
}
