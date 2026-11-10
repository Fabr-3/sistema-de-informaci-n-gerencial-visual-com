import { EnvironmentProviders, makeEnvironmentProviders } from '@angular/core';

import { RepositorioMovimientos } from '../dominio/repositorios/repositorio-movimientos';
import { RepositorioProveedores } from '../dominio/repositorios/repositorio-proveedores';
import { RepositorioMovimientosHttp } from './http/repositorio-movimientos-http';
import { RepositorioProveedoresHttp } from './http/repositorio-proveedores-http';

/** Registra las implementaciones HTTP de los repositorios del modulo financiero. */
export function proveerFinanzas(): EnvironmentProviders {
  return makeEnvironmentProviders([
    { provide: RepositorioProveedores, useClass: RepositorioProveedoresHttp },
    { provide: RepositorioMovimientos, useClass: RepositorioMovimientosHttp },
  ]);
}
