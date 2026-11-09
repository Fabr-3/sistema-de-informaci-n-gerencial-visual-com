import { WritableSignal } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { Observable } from 'rxjs';

/**
 * Ejecuta la operacion de guardado de un dialogo: marca el estado de
 * guardado, cierra el dialogo con el resultado o, si falla, informa el error
 * y permite reintentar.
 */
export function guardarYCerrar<T>(
  operacion: Observable<T>,
  dialogo: MatDialogRef<unknown, T>,
  guardando: WritableSignal<boolean>,
  alFallar: (error: unknown) => void,
): void {
  guardando.set(true);
  operacion.subscribe({
    next: (resultado) => dialogo.close(resultado),
    error: (error: unknown) => {
      alFallar(error);
      guardando.set(false);
    },
  });
}
