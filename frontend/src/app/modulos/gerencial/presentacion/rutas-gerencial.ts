import { Routes } from '@angular/router';

import { Rutas } from '../../../nucleo/configuracion/rutas';
import { Permisos } from '../../accesos/dominio/modelos/permisos';
import { permisoGuard } from '../../accesos/presentacion/guardias/guardias-sesion';

/** Rutas del modulo de informacion gerencial (carga diferida). */
export const RUTAS_GERENCIAL: Routes = [
  {
    path: '',
    canActivate: [permisoGuard(Permisos.REPORTES_VER)],
    children: [
      { path: '', pathMatch: 'full', redirectTo: Rutas.KPIS },
      {
        path: Rutas.KPIS,
        title: 'Tablero gerencial',
        loadComponent: () => import('./paginas/kpis/pagina-kpis').then((m) => m.PaginaKpis),
      },
      {
        path: Rutas.ALERTAS,
        title: 'Alertas de activos',
        loadComponent: () => import('./paginas/alertas/pagina-alertas').then((m) => m.PaginaAlertas),
      },
      {
        path: Rutas.REPORTES,
        title: 'Reportes operativos',
        loadComponent: () => import('./paginas/reportes/pagina-reportes').then((m) => m.PaginaReportes),
      },
    ],
  },
];
