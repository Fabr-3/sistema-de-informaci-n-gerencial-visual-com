import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { RepositorioProveedores } from '../dominio/repositorios/repositorio-proveedores';
import { GestionProveedores } from './gestion-proveedores';

describe('casos de uso de proveedores', () => {
  it('limpia los textos, normaliza el complemento y envia nulos en los campos vacios', () => {
    const repositorio = { guardar: vi.fn(() => of({})), listar: vi.fn(() => of({})) };
    TestBed.configureTestingModule({ providers: [{ provide: RepositorioProveedores, useValue: repositorio }] });
    const gestion = TestBed.inject(GestionProveedores);

    gestion
      .guardar(null, {
        idTipoProveedor: 1,
        razonSocial: ' Juan Mamani ',
        direccion: '  ',
        documentos: [{ id: null, idTipoDocumento: 1, numero: ' 4876512 ', complemento: ' 1a ' }],
        telefono: ' 70011223 ',
        email: '',
        servicios: ['Transporte'],
      })
      .subscribe();
    gestion.listar({ texto: '  ', activo: true }, { pagina: 0, tamano: 10 }).subscribe();

    expect(repositorio.guardar).toHaveBeenCalledWith(null, {
      idTipoProveedor: 1,
      razonSocial: 'Juan Mamani',
      direccion: null,
      documentos: [{ id: null, idTipoDocumento: 1, numero: '4876512', complemento: '1A' }],
      telefono: '70011223',
      email: null,
      servicios: ['Transporte'],
    });
    expect(repositorio.listar).toHaveBeenCalledWith({ texto: undefined, activo: true }, { pagina: 0, tamano: 10 });
  });
});
