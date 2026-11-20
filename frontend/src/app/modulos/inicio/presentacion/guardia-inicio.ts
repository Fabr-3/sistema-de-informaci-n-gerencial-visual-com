import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Rutas, rutaAbsoluta } from '../../../nucleo/configuracion/rutas';
import { ServicioSesion } from '../../accesos/aplicacion/servicio-sesion';
import { CodigosRol } from '../../accesos/dominio/modelos/rol';

/** El gerente empieza en su tablero de KPIs; los demas roles, en el inicio con accesos rapidos. */
export const inicioPorRolGuard: CanActivateFn = () =>
  inject(ServicioSesion).usuario()?.codigoRol === CodigosRol.GERENTE
    ? inject(Router).createUrlTree([rutaAbsoluta(Rutas.GERENCIAL, Rutas.KPIS)])
    : true;
