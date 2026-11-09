import { EnvironmentProviders, makeEnvironmentProviders } from '@angular/core';

import { RepositorioPersonal } from '../dominio/repositorios/repositorio-personal';
import { RepositorioPlanificacion } from '../dominio/repositorios/repositorio-planificacion';
import { RepositorioPersonalHttp } from './http/repositorio-personal-http';
import { RepositorioPlanificacionHttp } from './http/repositorio-planificacion-http';

/** Registra las implementaciones HTTP de los repositorios de logistica. */
export function proveerLogistica(): EnvironmentProviders {
  return makeEnvironmentProviders([
    { provide: RepositorioPlanificacion, useClass: RepositorioPlanificacionHttp },
    { provide: RepositorioPersonal, useClass: RepositorioPersonalHttp },
  ]);
}
