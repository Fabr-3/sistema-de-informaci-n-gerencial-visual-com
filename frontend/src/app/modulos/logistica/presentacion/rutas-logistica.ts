import { Routes } from '@angular/router';

import { PROVEEDOR_PAGINADOR_ESPANOL } from '../../../nucleo/configuracion/paginador-espanol';
import { Rutas } from '../../../nucleo/configuracion/rutas';
import { Permisos } from '../../accesos/dominio/modelos/permisos';
import { permisoGuard } from '../../accesos/presentacion/guardias/guardias-sesion';

/**
 * Rutas del modulo de logistica (carga diferida). La planificacion y el
 * personal requieren LOGISTICA_VER; las asignaciones propias, su permiso.
 */
export const RUTAS_LOGISTICA: Routes = [
  {
    path: '',
    providers: [PROVEEDOR_PAGINADOR_ESPANOL],
    children: [
      { path: '', pathMatch: 'full', redirectTo: Rutas.PLANIFICACION },
      {
        path: Rutas.PLANIFICACION,
        title: 'Montajes y personal',
        canActivate: [permisoGuard(Permisos.LOGISTICA_VER)],
        loadComponent: () =>
          import('./paginas/planificacion/pagina-planificacion').then((m) => m.PaginaPlanificacion),
      },
      {
        path: `${Rutas.PLANIFICACION}/:idEvento`,
        title: 'Plan logístico',
        canActivate: [permisoGuard(Permisos.LOGISTICA_VER)],
        loadComponent: () => import('./paginas/planificacion/pagina-plan-evento').then((m) => m.PaginaPlanEvento),
      },
      {
        path: Rutas.PERSONAL,
        title: 'Personal técnico',
        canActivate: [permisoGuard(Permisos.LOGISTICA_VER)],
        loadComponent: () => import('./paginas/personal/pagina-personal').then((m) => m.PaginaPersonal),
      },
      {
        path: Rutas.MIS_ASIGNACIONES,
        title: 'Mis asignaciones',
        canActivate: [permisoGuard(Permisos.ASIGNACIONES_VER_PROPIAS)],
        loadComponent: () =>
          import('./paginas/mis-asignaciones/pagina-mis-asignaciones').then((m) => m.PaginaMisAsignaciones),
      },
    ],
  },
];
