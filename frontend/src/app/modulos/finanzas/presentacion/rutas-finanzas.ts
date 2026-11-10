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
      { path: '', pathMatch: 'full', redirectTo: Rutas.PROVEEDORES },
      {
        path: Rutas.PROVEEDORES,
        title: 'Proveedores',
        canActivate: [permisoGuard(Permisos.FINANZAS_VER, Permisos.PROVEEDORES_VER)],
        loadComponent: () => import('./paginas/proveedores/pagina-proveedores').then((m) => m.PaginaProveedores),
      },
    ],
  },
];
