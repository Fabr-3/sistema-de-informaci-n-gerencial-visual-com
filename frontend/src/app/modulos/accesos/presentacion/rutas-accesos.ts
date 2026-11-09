import { MatPaginatorIntl } from '@angular/material/paginator';
import { Routes } from '@angular/router';

import { PaginadorEspanol } from '../../../nucleo/configuracion/paginador-espanol';
import { Rutas } from '../../../nucleo/configuracion/rutas';
import { Permisos } from '../dominio/modelos/permisos';
import { permisoGuard } from './guardias/guardias-sesion';

/** Rutas del modulo de administracion de accesos (carga diferida). */
export const RUTAS_ACCESOS: Routes = [
  {
    path: '',
    providers: [{ provide: MatPaginatorIntl, useClass: PaginadorEspanol }],
    children: [
      { path: '', pathMatch: 'full', redirectTo: Rutas.USUARIOS },
      {
        path: Rutas.USUARIOS,
        title: 'Usuarios',
        canActivate: [permisoGuard(Permisos.USUARIOS_VER)],
        loadComponent: () => import('./paginas/usuarios/pagina-usuarios').then((m) => m.PaginaUsuarios),
      },
      {
        path: Rutas.ROLES,
        title: 'Roles y permisos',
        canActivate: [permisoGuard(Permisos.ROLES_VER)],
        loadComponent: () => import('./paginas/roles/pagina-roles').then((m) => m.PaginaRoles),
      },
      {
        path: Rutas.AUDITORIA,
        title: 'Auditoría',
        canActivate: [permisoGuard(Permisos.AUDITORIA_VER)],
        loadComponent: () => import('./paginas/auditoria/pagina-auditoria').then((m) => m.PaginaAuditoria),
      },
      {
        path: Rutas.RESPALDOS,
        title: 'Respaldos',
        canActivate: [permisoGuard(Permisos.RESPALDOS_GESTIONAR)],
        loadComponent: () => import('./paginas/respaldos/pagina-respaldos').then((m) => m.PaginaRespaldos),
      },
    ],
  },
];
