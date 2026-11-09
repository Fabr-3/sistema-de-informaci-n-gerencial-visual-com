import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import localeEsBo from '@angular/common/locales/es-BO';
import {
  ApplicationConfig,
  LOCALE_ID,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { MAT_DATE_LOCALE, provideNativeDateAdapter } from '@angular/material/core';
import { MatIconRegistry } from '@angular/material/icon';
import { TitleStrategy, provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';
import { ServicioSesion } from './modulos/accesos/aplicacion/servicio-sesion';
import { interceptorAutenticacion } from './modulos/accesos/infraestructura/http/interceptor-autenticacion';
import { proveerAccesos } from './modulos/accesos/infraestructura/proveedores-accesos';
import { proveerActivos } from './modulos/activos/infraestructura/proveedores-activos';
import { proveerComercial } from './modulos/comercial/infraestructura/proveedores-comercial';
import { proveerLogistica } from './modulos/logistica/infraestructura/proveedores-logistica';
import { EstrategiaTitulo } from './nucleo/configuracion/estrategia-titulo';

const LOCALE = 'es-BO';
const CLASE_FUENTE_ICONOS = 'material-symbols-outlined';

registerLocaleData(localeEsBo);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([interceptorAutenticacion])),
    provideNativeDateAdapter(),
    proveerAccesos(),
    proveerActivos(),
    proveerComercial(),
    proveerLogistica(),
    { provide: LOCALE_ID, useValue: LOCALE },
    { provide: MAT_DATE_LOCALE, useValue: LOCALE },
    { provide: TitleStrategy, useClass: EstrategiaTitulo },
    provideAppInitializer(() => {
      inject(MatIconRegistry).setDefaultFontSetClass(CLASE_FUENTE_ICONOS);
      // Recupera la sesion (cookie de refresco) antes de evaluar las guardias
      return inject(ServicioSesion).restaurar();
    }),
  ],
};
