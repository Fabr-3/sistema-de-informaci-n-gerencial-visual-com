import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { RepositorioConsumibles, RepositorioSubcontrataciones } from '../dominio/repositorios/repositorios-subcontratacion';
import { GestionConsumibles, GestionSubcontrataciones } from './gestion-subcontratacion';

describe('casos de uso de subcontratacion', () => {
  it('limpia los textos antes de enviarlos', () => {
    const subcontrataciones = { registrar: vi.fn(() => of({})), anular: vi.fn(() => of({})) };
    const consumibles = { guardar: vi.fn(() => of({})), ajustar: vi.fn(() => of({})) };
    TestBed.configureTestingModule({
      providers: [
        { provide: RepositorioSubcontrataciones, useValue: subcontrataciones },
        { provide: RepositorioConsumibles, useValue: consumibles },
      ],
    });

    TestBed.inject(GestionSubcontrataciones).anular(4, '  Proveedor ausente ').subscribe();
    TestBed.inject(GestionConsumibles)
      .guardar(null, { nombre: ' Cinta gaffer ', unidadMedida: ' rollo ', stockMinimo: 2, costoUnitario: 45, activo: true })
      .subscribe();
    TestBed.inject(GestionConsumibles).ajustar(1, { cantidad: -2, motivo: ' Conteo físico ' }).subscribe();

    expect(subcontrataciones.anular).toHaveBeenCalledWith(4, 'Proveedor ausente');
    expect(consumibles.guardar).toHaveBeenCalledWith(null, expect.objectContaining({ nombre: 'Cinta gaffer', unidadMedida: 'rollo' }));
    expect(consumibles.ajustar).toHaveBeenCalledWith(1, { cantidad: -2, motivo: 'Conteo físico' });
  });
});
