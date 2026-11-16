import { EnvironmentProviders, makeEnvironmentProviders } from '@angular/core';

import { RepositorioPersonal } from '../dominio/repositorios/repositorio-personal';
import { RepositorioPlanificacion } from '../dominio/repositorios/repositorio-planificacion';
import { RepositorioReservas } from '../dominio/repositorios/repositorio-reservas';
import { RepositorioPersonalHttp } from './http/repositorio-personal-http';
import { RepositorioPlanificacionHttp } from './http/repositorio-planificacion-http';
import { RepositorioReservasHttp } from './http/repositorio-reservas-http';

/** Registra las implementaciones HTTP de los repositorios de logistica. */
export function proveerLogistica(): EnvironmentProviders {
  return makeEnvironmentProviders([
    { provide: RepositorioPlanificacion, useClass: RepositorioPlanificacionHttp },
    { provide: RepositorioPersonal, useClass: RepositorioPersonalHttp },
    { provide: RepositorioReservas, useClass: RepositorioReservasHttp },
  ]);
}
