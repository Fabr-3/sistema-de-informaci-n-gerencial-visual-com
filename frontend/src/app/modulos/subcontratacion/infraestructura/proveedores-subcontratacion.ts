import { EnvironmentProviders, makeEnvironmentProviders } from '@angular/core';

import {
  RepositorioConsumibles,
  RepositorioConsumos,
  RepositorioGastos,
  RepositorioSubcontrataciones,
} from '../dominio/repositorios/repositorios-subcontratacion';
import {
  RepositorioConsumiblesHttp,
  RepositorioConsumosHttp,
  RepositorioGastosHttp,
  RepositorioSubcontratacionesHttp,
} from './http/repositorios-subcontratacion-http';

/** Registra las implementaciones HTTP de los repositorios de subcontratacion y consumibles. */
export function proveerSubcontratacion(): EnvironmentProviders {
  return makeEnvironmentProviders([
    { provide: RepositorioSubcontrataciones, useClass: RepositorioSubcontratacionesHttp },
    { provide: RepositorioConsumibles, useClass: RepositorioConsumiblesHttp },
    { provide: RepositorioConsumos, useClass: RepositorioConsumosHttp },
    { provide: RepositorioGastos, useClass: RepositorioGastosHttp },
  ]);
}
