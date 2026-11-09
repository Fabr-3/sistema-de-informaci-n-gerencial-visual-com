import { EnvironmentProviders, Provider, makeEnvironmentProviders } from '@angular/core';

import { RepositorioAuditoria } from '../dominio/repositorios/repositorio-auditoria';
import { RepositorioAutenticacion } from '../dominio/repositorios/repositorio-autenticacion';
import { RepositorioRespaldos } from '../dominio/repositorios/repositorio-respaldos';
import { RepositorioRoles } from '../dominio/repositorios/repositorio-roles';
import { RepositorioUsuarios } from '../dominio/repositorios/repositorio-usuarios';
import { RepositorioAuditoriaHttp } from './http/repositorio-auditoria-http';
import { RepositorioAutenticacionHttp } from './http/repositorio-autenticacion-http';
import { RepositorioRespaldosHttp } from './http/repositorio-respaldos-http';
import { RepositorioRolesHttp } from './http/repositorio-roles-http';
import { RepositorioUsuariosHttp } from './http/repositorio-usuarios-http';

/** Asocia los puertos del dominio con sus implementaciones HTTP. */
const ADAPTADORES: Provider[] = [
  { provide: RepositorioAutenticacion, useClass: RepositorioAutenticacionHttp },
  { provide: RepositorioUsuarios, useClass: RepositorioUsuariosHttp },
  { provide: RepositorioRoles, useClass: RepositorioRolesHttp },
  { provide: RepositorioAuditoria, useClass: RepositorioAuditoriaHttp },
  { provide: RepositorioRespaldos, useClass: RepositorioRespaldosHttp },
];

export function proveerAccesos(): EnvironmentProviders {
  return makeEnvironmentProviders(ADAPTADORES);
}
