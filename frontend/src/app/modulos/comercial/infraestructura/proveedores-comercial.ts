import { EnvironmentProviders, makeEnvironmentProviders } from '@angular/core';

import { RepositorioCatalogosComerciales } from '../dominio/repositorios/repositorio-catalogos-comerciales';
import { RepositorioClientes } from '../dominio/repositorios/repositorio-clientes';
import { RepositorioCotizaciones } from '../dominio/repositorios/repositorio-cotizaciones';
import { RepositorioProyectos } from '../dominio/repositorios/repositorio-proyectos';
import { RepositorioCatalogosComercialesHttp } from './http/repositorio-catalogos-comerciales-http';
import { RepositorioClientesHttp } from './http/repositorio-clientes-http';
import { RepositorioCotizacionesHttp } from './http/repositorio-cotizaciones-http';
import { RepositorioProyectosHttp } from './http/repositorio-proyectos-http';

/** Asocia los puertos del modulo comercial con sus implementaciones HTTP. */
export function proveerComercial(): EnvironmentProviders {
  return makeEnvironmentProviders([
    { provide: RepositorioClientes, useClass: RepositorioClientesHttp },
    { provide: RepositorioCatalogosComerciales, useClass: RepositorioCatalogosComercialesHttp },
    { provide: RepositorioCotizaciones, useClass: RepositorioCotizacionesHttp },
    { provide: RepositorioProyectos, useClass: RepositorioProyectosHttp },
  ]);
}
