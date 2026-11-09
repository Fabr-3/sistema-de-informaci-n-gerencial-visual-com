import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { RepositorioActivos } from '../dominio/repositorios/repositorio-activos';
import { RepositorioCatalogosActivos } from '../dominio/repositorios/repositorio-catalogos-activos';
import { activo, modelo } from '../pruebas/datos-prueba';
import { CatalogosActivos } from './catalogos-activos';
import { GestionActivos } from './gestion-activos';

describe('casos de uso de activos', () => {
  const repositorioActivos = { registrar: vi.fn(() => of([activo()])) };
  const repositorioCatalogos = { guardarModelo: vi.fn(() => of(modelo())) };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: RepositorioActivos, useValue: repositorioActivos },
        { provide: RepositorioCatalogosActivos, useValue: repositorioCatalogos },
      ],
    });
  });

  const registro = {
    idModelo: 1,
    idBodega: 1,
    cantidad: 2,
    codigoInventario: '  ',
    numerosSerie: [' SN-1 ', '', 'SN-2'],
    fechaAdquisicion: null,
    costoUnitario: 1500,
    observaciones: null,
  };

  it('normaliza el codigo manual y descarta series vacias', () => {
    const gestion = TestBed.inject(GestionActivos);

    gestion.registrar(registro).subscribe();
    gestion.registrar({ ...registro, cantidad: 1, codigoInventario: ' led-0002 ', numerosSerie: [] }).subscribe();

    expect(repositorioActivos.registrar).toHaveBeenNthCalledWith(
      1,
      expect.objectContaining({ codigoInventario: null, numerosSerie: ['SN-1', 'SN-2'] }),
    );
    expect(repositorioActivos.registrar).toHaveBeenNthCalledWith(2, expect.objectContaining({ codigoInventario: 'LED-0002' }));
  });

  it('los modelos por lote no envian intervalo de mantenimiento', () => {
    TestBed.inject(CatalogosActivos)
      .guardarModelo(null, {
        idFabricante: 1,
        idCategoria: 1,
        nombre: ' Cable DMX ',
        tipoControl: 'LOTE',
        horasMantenimiento: 100,
        prefijoCodigo: 'cab',
        especificaciones: [],
      })
      .subscribe();

    expect(repositorioCatalogos.guardarModelo).toHaveBeenCalledWith(
      null,
      expect.objectContaining({ nombre: 'Cable DMX', horasMantenimiento: null, prefijoCodigo: null }),
    );
  });
});
