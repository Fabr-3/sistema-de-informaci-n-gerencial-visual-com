import { TestBed } from '@angular/core/testing';

import { Permiso } from '../dominio/modelos/permisos';
import { RepositorioRoles } from '../dominio/repositorios/repositorio-roles';
import { GestionRoles } from './gestion-roles';

describe('GestionRoles', () => {
  it('agrupa los permisos por modulo con su nombre visible', () => {
    TestBed.configureTestingModule({ providers: [{ provide: RepositorioRoles, useValue: {} }] });
    const permisos: Permiso[] = [
      { id: 1, modulo: 'ACCESOS', nombre: 'USUARIOS_VER', descripcion: null },
      { id: 2, modulo: 'ACCESOS', nombre: 'ROLES_VER', descripcion: null },
      { id: 3, modulo: 'FINANZAS', nombre: 'FINANZAS_VER', descripcion: null },
    ];

    const grupos = TestBed.inject(GestionRoles).agruparPorModulo(permisos);

    expect(grupos.map((grupo) => grupo.nombreModulo)).toEqual(['Administración de accesos', 'Financiero']);
    expect(grupos[0].permisos.map((permiso) => permiso.id)).toEqual([1, 2]);
  });
});
