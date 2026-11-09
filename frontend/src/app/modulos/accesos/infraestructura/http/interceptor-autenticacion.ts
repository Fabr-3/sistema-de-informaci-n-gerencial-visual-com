import { HttpErrorResponse, HttpInterceptorFn, HttpRequest, HttpStatusCode } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';

import { URL_API } from '../../../../nucleo/configuracion/api';
import { Rutas, rutaAbsoluta } from '../../../../nucleo/configuracion/rutas';
import { ServicioSesion } from '../../aplicacion/servicio-sesion';
import { RUTAS_PUBLICAS_AUTENTICACION } from './repositorio-autenticacion-http';

/**
 * Agrega el token de acceso a las llamadas a la API. Ante un 401 intenta un
 * unico refresco de sesion y repite la solicitud; si falla, envia al usuario
 * a la pantalla de inicio de sesion.
 */
export const interceptorAutenticacion: HttpInterceptorFn = (solicitud, siguiente) => {
  const urlApi = inject(URL_API);
  if (!solicitud.url.startsWith(urlApi) || esRutaPublica(solicitud.url, urlApi)) {
    return siguiente(solicitud);
  }
  const sesion = inject(ServicioSesion);
  const router = inject(Router);

  return siguiente(conToken(solicitud, sesion.tokenAcceso())).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== HttpStatusCode.Unauthorized) {
        return throwError(() => error);
      }
      return sesion.refrescar().pipe(
        catchError(() => {
          sesion.finalizarLocalmente();
          void router.navigateByUrl(rutaAbsoluta(Rutas.INICIO_SESION));
          return throwError(() => error);
        }),
        switchMap((renovada) => siguiente(conToken(solicitud, renovada.tokenAcceso))),
      );
    }),
  );
};

function esRutaPublica(url: string, urlApi: string): boolean {
  return RUTAS_PUBLICAS_AUTENTICACION.some((ruta) => url === urlApi + ruta);
}

function conToken(solicitud: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  return token ? solicitud.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : solicitud;
}
