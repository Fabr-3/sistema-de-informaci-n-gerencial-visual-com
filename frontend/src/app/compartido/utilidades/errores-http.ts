import { HttpErrorResponse } from '@angular/common/http';

export const MENSAJE_SIN_CONEXION = 'No se pudo conectar con el servidor';
export const MENSAJE_ERROR_GENERICO = 'Ocurrio un error inesperado. Intente nuevamente';

/** Cuerpo de error RFC 9457 que devuelve la API. */
interface DetalleProblema {
  readonly detail?: string;
  readonly errores?: Record<string, string>;
}

/**
 * Obtiene un mensaje legible para el usuario a partir de un error HTTP. Usa
 * el detalle de negocio de la API y, si existen, el primer error de campo.
 */
export function mensajeDeError(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return MENSAJE_ERROR_GENERICO;
  }
  if (error.status === 0) {
    return MENSAJE_SIN_CONEXION;
  }
  const problema = error.error as DetalleProblema | null;
  if (!problema?.detail) {
    return MENSAJE_ERROR_GENERICO;
  }
  const primerErrorCampo = problema.errores ? Object.entries(problema.errores)[0] : undefined;
  return primerErrorCampo ? `${problema.detail}: ${primerErrorCampo[0]} ${primerErrorCampo[1]}` : problema.detail;
}
