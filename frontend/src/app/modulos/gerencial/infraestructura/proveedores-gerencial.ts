import { EnvironmentProviders, makeEnvironmentProviders } from '@angular/core';

import { RepositorioGerencial } from '../dominio/repositorios/repositorio-gerencial';
import { RepositorioGerencialHttp } from './http/repositorio-gerencial-http';

/** Registra la implementacion HTTP del repositorio de informacion gerencial. */
export function proveerGerencial(): EnvironmentProviders {
  return makeEnvironmentProviders([{ provide: RepositorioGerencial, useClass: RepositorioGerencialHttp }]);
}
