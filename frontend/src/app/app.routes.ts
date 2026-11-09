import { Routes } from '@angular/router';

import {
  autenticacionGuard,
  invitadoGuard,
  sesionIniciadaGuard,
} from './modulos/accesos/presentacion/guardias/guardias-sesion';
import { Rutas } from './nucleo/configuracion/rutas';

export const routes: Routes = [
  {
    path: Rutas.INICIO_SESION,
    title: 'Iniciar sesión',
    canActivate: [invitadoGuard],
    loadComponent: () =>
      import('./modulos/accesos/presentacion/paginas/inicio-sesion/pagina-inicio-sesion').then(
        (m) => m.PaginaInicioSesion,
      ),
  },
  {
    path: Rutas.CAMBIO_CONTRASENA,
    title: 'Cambiar contraseña',
    canActivate: [sesionIniciadaGuard],
    loadComponent: () =>
      import('./modulos/accesos/presentacion/paginas/cambio-contrasena/pagina-cambio-contrasena').then(
        (m) => m.PaginaCambioContrasena,
      ),
  },
  {
    path: Rutas.INICIO,
    loadComponent: () =>
      import('./nucleo/diseno/plantilla-principal/plantilla-principal').then((m) => m.PlantillaPrincipal),
    canActivate: [autenticacionGuard],
    canActivateChild: [autenticacionGuard],
    children: [
      {
        path: Rutas.INICIO,
        pathMatch: 'full',
        title: 'Inicio',
        loadComponent: () =>
          import('./modulos/inicio/presentacion/pagina-inicio/pagina-inicio').then((m) => m.PaginaInicio),
      },
      {
        path: Rutas.ACCESOS,
        loadChildren: () => import('./modulos/accesos/presentacion/rutas-accesos').then((m) => m.RUTAS_ACCESOS),
      },
      {
        path: Rutas.COMERCIAL,
        loadChildren: () => import('./modulos/comercial/presentacion/rutas-comercial').then((m) => m.RUTAS_COMERCIAL),
      },
      {
        path: Rutas.LOGISTICA,
        loadChildren: () => import('./modulos/logistica/presentacion/rutas-logistica').then((m) => m.RUTAS_LOGISTICA),
      },
      {
        path: Rutas.ACTIVOS,
        loadChildren: () => import('./modulos/activos/presentacion/rutas-activos').then((m) => m.RUTAS_ACTIVOS),
      },
    ],
  },
  { path: '**', redirectTo: Rutas.INICIO },
];
