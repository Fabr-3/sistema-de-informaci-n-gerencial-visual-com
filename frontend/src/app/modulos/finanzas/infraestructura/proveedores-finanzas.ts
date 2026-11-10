import { EnvironmentProviders, makeEnvironmentProviders } from '@angular/core';

import { RepositorioProveedores } from '../dominio/repositorios/repositorio-proveedores';
import { RepositorioProveedoresHttp } from './http/repositorio-proveedores-http';

/** Registra las implementaciones HTTP de los repositorios del modulo financiero. */
export function proveerFinanzas(): EnvironmentProviders {
  return makeEnvironmentProviders([{ provide: RepositorioProveedores, useClass: RepositorioProveedoresHttp }]);
}
