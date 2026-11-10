import { ValidatorFn } from '@angular/forms';

const esElementoElegido = (valor: unknown): boolean =>
  typeof valor === 'object' && valor !== null && typeof (valor as { id?: unknown }).id === 'number';

/** Exige que el control tenga un elemento elegido de la lista (con id), no texto libre. */
export const requiereSeleccion: ValidatorFn = (control) =>
  esElementoElegido(control.value) ? null : { seleccionRequerida: true };

/** Campo opcional: vacio o un elemento elegido de la lista, nunca texto libre a medias. */
export const seleccionOpcional: ValidatorFn = (control) => {
  const valor: unknown = control.value;
  return valor === null || valor === '' || esElementoElegido(valor) ? null : { seleccionRequerida: true };
};
