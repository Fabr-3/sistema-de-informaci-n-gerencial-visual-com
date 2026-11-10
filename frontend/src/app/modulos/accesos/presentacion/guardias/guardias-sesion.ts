import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { PARAMETRO_RETORNO, Rutas, rutaAbsoluta } from '../../../../nucleo/configuracion/rutas';
import { ServicioSesion } from '../../aplicacion/servicio-sesion';

/**
 * Exige sesion iniciada. Si la contrasena es temporal, solo permite la
 * pantalla de cambio de contrasena.
 */
export const autenticacionGuard: CanActivateFn = (_ruta, estado) => {
  const sesion = inject(ServicioSesion);
  const router = inject(Router);
  if (!sesion.estaAutenticado()) {
    return router.createUrlTree([rutaAbsoluta(Rutas.INICIO_SESION)], {
      queryParams: { [PARAMETRO_RETORNO]: estado.url },
    });
  }
  if (sesion.debeCambiarContrasena()) {
    return router.createUrlTree([rutaAbsoluta(Rutas.CAMBIO_CONTRASENA)]);
  }
  return true;
};

/** Para la pantalla de cambio de contrasena: basta con tener sesion. */
export const sesionIniciadaGuard: CanActivateFn = () => {
  const sesion = inject(ServicioSesion);
  return sesion.estaAutenticado() || inject(Router).createUrlTree([rutaAbsoluta(Rutas.INICIO_SESION)]);
};

/** Evita mostrar el inicio de sesion a quien ya esta autenticado. */
export const invitadoGuard: CanActivateFn = () => {
  const sesion = inject(ServicioSesion);
  return !sesion.estaAutenticado() || inject(Router).createUrlTree([rutaAbsoluta(Rutas.INICIO)]);
};

/** Restringe una ruta a quienes tienen alguno de los permisos indicados. */
export function permisoGuard(...permisos: string[]): CanActivateFn {
  return () => {
    const sesion = inject(ServicioSesion);
    return sesion.tieneAlgunPermiso(...permisos) || inject(Router).createUrlTree([rutaAbsoluta(Rutas.INICIO)]);
  };
}
