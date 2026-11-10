import { AbstractControl } from '@angular/forms';
import { Observable, catchError, debounceTime, distinctUntilChanged, filter, of, startWith, switchMap } from 'rxjs';

/** Espera tras cada tecla antes de consultar al servidor. */
export const ESPERA_BUSQUEDA_MS = 300;

/**
 * Sugerencias de un autocompletado: consulta mientras se escribe y no
 * vuelve a consultar cuando el valor es un elemento ya elegido.
 */
export function busquedaRemota<T>(
  control: AbstractControl<unknown>,
  buscar: (texto: string) => Observable<T[]>,
): Observable<T[]> {
  return control.valueChanges.pipe(
    startWith(''),
    filter((valor): valor is string => typeof valor === 'string'),
    debounceTime(ESPERA_BUSQUEDA_MS),
    distinctUntilChanged(),
    switchMap((texto) => buscar(texto).pipe(catchError(() => of<T[]>([])))),
  );
}
