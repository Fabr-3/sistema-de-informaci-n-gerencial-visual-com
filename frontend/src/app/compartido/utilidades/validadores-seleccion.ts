import { ValidatorFn } from '@angular/forms';

/** Exige que el control tenga un elemento elegido de la lista (con id), no texto libre. */
export const requiereSeleccion: ValidatorFn = (control) => {
  const valor: unknown = control.value;
  const elegido = typeof valor === 'object' && valor !== null && typeof (valor as { id?: unknown }).id === 'number';
  return elegido ? null : { seleccionRequerida: true };
};
