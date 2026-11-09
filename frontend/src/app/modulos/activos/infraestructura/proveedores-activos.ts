import { EnvironmentProviders, makeEnvironmentProviders } from '@angular/core';

import { RepositorioActivos } from '../dominio/repositorios/repositorio-activos';
import { RepositorioCatalogosActivos } from '../dominio/repositorios/repositorio-catalogos-activos';
import { RepositorioHistorialActivos } from '../dominio/repositorios/repositorio-historial-activos';
import { RepositorioLotes } from '../dominio/repositorios/repositorio-lotes';
import { RepositorioActivosHttp } from './http/repositorio-activos-http';
import { RepositorioCatalogosActivosHttp } from './http/repositorio-catalogos-activos-http';
import { RepositorioHistorialActivosHttp } from './http/repositorio-historial-activos-http';
import { RepositorioLotesHttp } from './http/repositorio-lotes-http';

/** Asocia los puertos del modulo de activos con sus implementaciones HTTP. */
export function proveerActivos(): EnvironmentProviders {
  return makeEnvironmentProviders([
    { provide: RepositorioActivos, useClass: RepositorioActivosHttp },
    { provide: RepositorioHistorialActivos, useClass: RepositorioHistorialActivosHttp },
    { provide: RepositorioCatalogosActivos, useClass: RepositorioCatalogosActivosHttp },
    { provide: RepositorioLotes, useClass: RepositorioLotesHttp },
  ]);
}
