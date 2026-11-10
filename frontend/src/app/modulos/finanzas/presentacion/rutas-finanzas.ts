import { Routes } from '@angular/router';

import { PROVEEDOR_PAGINADOR_ESPANOL } from '../../../nucleo/configuracion/paginador-espanol';
import { Rutas } from '../../../nucleo/configuracion/rutas';
import { Permisos } from '../../accesos/dominio/modelos/permisos';
import { permisoGuard } from '../../accesos/presentacion/guardias/guardias-sesion';

/** Rutas del modulo financiero (carga diferida). */
export const RUTAS_FINANZAS: Routes = [
  {
    path: '',
    providers: [PROVEEDOR_PAGINADOR_ESPANOL],
    children: [
      { path: '', pathMatch: 'full', redirectTo: Rutas.INDICADORES },
      {
        path: Rutas.INDICADORES,
        title: 'Indicadores financieros',
        canActivate: [permisoGuard(Permisos.FINANZAS_VER)],
        loadComponent: () => import('./paginas/indicadores/pagina-indicadores').then((m) => m.PaginaIndicadores),
      },
      {
        path: Rutas.MOVIMIENTOS,
        title: 'Ingresos y egresos',
        canActivate: [permisoGuard(Permisos.FINANZAS_VER)],
        loadComponent: () => import('./paginas/movimientos/pagina-movimientos').then((m) => m.PaginaMovimientos),
      },
      {
        path: `${Rutas.MOVIMIENTOS}/${Rutas.NUEVA}`,
        title: 'Registrar movimiento',
        canActivate: [permisoGuard(Permisos.FINANZAS_GESTIONAR)],
        loadComponent: () => import('./paginas/movimientos/registro-movimiento').then((m) => m.RegistroMovimiento),
      },
      {
        path: Rutas.RESULTADOS,
        title: 'Resultados por proyecto',
        canActivate: [permisoGuard(Permisos.FINANZAS_VER)],
        loadComponent: () => import('./paginas/resultados/pagina-resultados').then((m) => m.PaginaResultados),
      },
      {
        path: `${Rutas.RESULTADOS}/:id`,
        title: 'Resultado del proyecto',
        canActivate: [permisoGuard(Permisos.FINANZAS_VER)],
        loadComponent: () => import('./paginas/resultados/detalle-resultado').then((m) => m.DetalleResultado),
      },
      {
        path: Rutas.PROVEEDORES,
        title: 'Proveedores',
        canActivate: [permisoGuard(Permisos.FINANZAS_VER, Permisos.PROVEEDORES_VER)],
        loadComponent: () => import('./paginas/proveedores/pagina-proveedores').then((m) => m.PaginaProveedores),
      },
    ],
  },
];
