import { Routes } from '@angular/router';

import { PROVEEDOR_PAGINADOR_ESPANOL } from '../../../nucleo/configuracion/paginador-espanol';
import { Rutas } from '../../../nucleo/configuracion/rutas';
import { Permisos } from '../../accesos/dominio/modelos/permisos';
import { permisoGuard } from '../../accesos/presentacion/guardias/guardias-sesion';

/** Rutas del modulo de subcontratacion y consumibles (carga diferida). */
export const RUTAS_SUBCONTRATACION: Routes = [
  {
    path: '',
    providers: [PROVEEDOR_PAGINADOR_ESPANOL],
    children: [
      { path: '', pathMatch: 'full', redirectTo: Rutas.SUBCONTRATACIONES },
      {
        path: Rutas.SUBCONTRATACIONES,
        title: 'Subcontrataciones',
        canActivate: [permisoGuard(Permisos.PROVEEDORES_VER, Permisos.PROVEEDORES_GESTIONAR)],
        loadComponent: () =>
          import('./paginas/subcontrataciones/pagina-subcontrataciones').then((m) => m.PaginaSubcontrataciones),
      },
      {
        path: Rutas.CONSUMIBLES,
        title: 'Consumibles',
        canActivate: [
          permisoGuard(Permisos.PROVEEDORES_VER, Permisos.PROVEEDORES_GESTIONAR, Permisos.CONSUMIBLES_REGISTRAR),
        ],
        loadComponent: () => import('./paginas/consumibles/pagina-consumibles').then((m) => m.PaginaConsumibles),
      },
      {
        path: Rutas.CONSUMOS,
        title: 'Consumos',
        canActivate: [
          permisoGuard(Permisos.PROVEEDORES_VER, Permisos.PROVEEDORES_GESTIONAR, Permisos.CONSUMIBLES_REGISTRAR),
        ],
        loadComponent: () => import('./paginas/consumos/pagina-consumos').then((m) => m.PaginaConsumos),
      },
    ],
  },
];
