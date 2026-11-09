import { Routes } from '@angular/router';
import { CalendarDateFormatter, CalendarNativeDateFormatter, DateAdapter, provideCalendar } from 'angular-calendar';
import { adapterFactory } from 'angular-calendar/date-adapters/date-fns';

import { PROVEEDOR_PAGINADOR_ESPANOL } from '../../../nucleo/configuracion/paginador-espanol';
import { Rutas } from '../../../nucleo/configuracion/rutas';
import { Permisos } from '../../accesos/dominio/modelos/permisos';
import { permisoGuard } from '../../accesos/presentacion/guardias/guardias-sesion';

/**
 * Rutas del modulo comercial (carga diferida). Todas requieren COMERCIAL_VER;
 * las acciones de registro y aprobacion se controlan en cada pagina y en el backend.
 */
export const RUTAS_COMERCIAL: Routes = [
  {
    path: '',
    providers: [
      PROVEEDOR_PAGINADOR_ESPANOL,
      provideCalendar(
        { provide: DateAdapter, useFactory: adapterFactory },
        // Intl respeta el orden dia-mes del espanol en titulos y cabeceras
        { dateFormatter: { provide: CalendarDateFormatter, useClass: CalendarNativeDateFormatter } },
      ),
    ],
    canActivate: [permisoGuard(Permisos.COMERCIAL_VER)],
    children: [
      { path: '', pathMatch: 'full', redirectTo: Rutas.COTIZACIONES },
      {
        path: Rutas.CLIENTES,
        title: 'Clientes',
        loadComponent: () => import('./paginas/clientes/pagina-clientes').then((m) => m.PaginaClientes),
      },
      {
        path: Rutas.COTIZACIONES,
        title: 'Cotizaciones',
        loadComponent: () => import('./paginas/cotizaciones/pagina-cotizaciones').then((m) => m.PaginaCotizaciones),
      },
      {
        path: `${Rutas.COTIZACIONES}/${Rutas.NUEVA}`,
        title: 'Nueva cotización',
        canActivate: [permisoGuard(Permisos.COMERCIAL_GESTIONAR)],
        loadComponent: () => import('./paginas/cotizaciones/editor-cotizacion').then((m) => m.EditorCotizacion),
      },
      {
        path: `${Rutas.COTIZACIONES}/:id`,
        title: 'Cotización',
        loadComponent: () => import('./paginas/cotizaciones/editor-cotizacion').then((m) => m.EditorCotizacion),
      },
      {
        path: Rutas.PROYECTOS,
        title: 'Proyectos',
        loadComponent: () => import('./paginas/proyectos/pagina-proyectos').then((m) => m.PaginaProyectos),
      },
      {
        path: `${Rutas.PROYECTOS}/:id`,
        title: 'Proyecto',
        loadComponent: () => import('./paginas/proyectos/detalle-proyecto').then((m) => m.PaginaDetalleProyecto),
      },
      {
        path: Rutas.CRONOGRAMA,
        title: 'Cronograma',
        loadComponent: () => import('./paginas/cronograma/pagina-cronograma').then((m) => m.PaginaCronograma),
      },
      {
        path: Rutas.CATALOGOS,
        title: 'Servicios y lugares',
        loadComponent: () =>
          import('./paginas/catalogos/pagina-catalogos-comerciales').then((m) => m.PaginaCatalogosComerciales),
      },
    ],
  },
];
