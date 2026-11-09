import { InjectionToken } from '@angular/core';

/**
 * Prefijo de la API. En desarrollo el servidor de Angular reenvia /api al
 * backend (proxy.conf.json), por lo que frontend y API comparten origen y la
 * cookie de refresco viaja sin configuracion CORS.
 */
export const URL_API = new InjectionToken<string>('URL_API', { factory: () => '/api' });
