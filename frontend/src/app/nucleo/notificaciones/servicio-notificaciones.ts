import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

import { mensajeDeError } from '../../compartido/utilidades/errores-http';

const DURACION_EXITO_MS = 3000;
const DURACION_ERROR_MS = 6000;
const TEXTO_CERRAR = 'Cerrar';

/** Mensajes breves de resultado de operaciones. */
@Injectable({ providedIn: 'root' })
export class ServicioNotificaciones {
  private readonly snackBar = inject(MatSnackBar);

  exito(mensaje: string): void {
    this.snackBar.open(mensaje, TEXTO_CERRAR, { duration: DURACION_EXITO_MS });
  }

  error(error: unknown): void {
    this.snackBar.open(mensajeDeError(error), TEXTO_CERRAR, {
      duration: DURACION_ERROR_MS,
      panelClass: 'snack-error',
    });
  }
}
