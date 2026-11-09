import { Routes } from '@angular/router';

import { PROVEEDOR_PAGINADOR_ESPANOL } from '../../../nucleo/configuracion/paginador-espanol';
import { Rutas } from '../../../nucleo/configuracion/rutas';
import { Permisos } from '../../accesos/dominio/modelos/permisos';
import { permisoGuard } from '../../accesos/presentacion/guardias/guardias-sesion';

/**
 * Rutas del modulo de activos (carga diferida). Todas requieren ACTIVOS_VER;
 * las acciones de modificacion se controlan en cada pagina y en el backend.
 */
export const RUTAS_ACTIVOS: Routes = [
  {
    path: '',
    providers: [PROVEEDOR_PAGINADOR_ESPANOL],
    canActivate: [permisoGuard(Permisos.ACTIVOS_VER)],
    children: [
      {
        path: '',
        pathMatch: 'full',
        title: 'Activos',
        loadComponent: () => import('./paginas/activos/pagina-activos').then((m) => m.PaginaActivos),
      },
      {
        path: Rutas.LOTES,
        title: 'Inventario por lotes',
        loadComponent: () => import('./paginas/lotes/pagina-lotes').then((m) => m.PaginaLotes),
      },
      {
        path: Rutas.CATALOGOS,
        title: 'Catálogos de activos',
        loadComponent: () => import('./paginas/catalogos/pagina-catalogos').then((m) => m.PaginaCatalogos),
      },
      {
        path: ':id',
        title: 'Detalle del activo',
        loadComponent: () =>
          import('./paginas/detalle-activo/pagina-detalle-activo').then((m) => m.PaginaDetalleActivo),
      },
    ],
  },
];
