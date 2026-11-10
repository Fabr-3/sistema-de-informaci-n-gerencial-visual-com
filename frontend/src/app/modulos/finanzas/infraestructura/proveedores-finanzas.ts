import { EnvironmentProviders, makeEnvironmentProviders } from '@angular/core';

import { RepositorioIndicadores } from '../dominio/repositorios/repositorio-indicadores';
import { RepositorioMovimientos } from '../dominio/repositorios/repositorio-movimientos';
import { RepositorioProveedores } from '../dominio/repositorios/repositorio-proveedores';
import { RepositorioResultados } from '../dominio/repositorios/repositorio-resultados';
import { RepositorioIndicadoresHttp } from './http/repositorio-indicadores-http';
import { RepositorioMovimientosHttp } from './http/repositorio-movimientos-http';
import { RepositorioResultadosHttp } from './http/repositorio-resultados-http';
import { RepositorioProveedoresHttp } from './http/repositorio-proveedores-http';

/** Registra las implementaciones HTTP de los repositorios del modulo financiero. */
export function proveerFinanzas(): EnvironmentProviders {
  return makeEnvironmentProviders([
    { provide: RepositorioProveedores, useClass: RepositorioProveedoresHttp },
    { provide: RepositorioMovimientos, useClass: RepositorioMovimientosHttp },
    { provide: RepositorioResultados, useClass: RepositorioResultadosHttp },
    { provide: RepositorioIndicadores, useClass: RepositorioIndicadoresHttp },
  ]);
}
