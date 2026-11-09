import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { RepositorioCatalogosComerciales } from '../dominio/repositorios/repositorio-catalogos-comerciales';
import { RepositorioClientes } from '../dominio/repositorios/repositorio-clientes';
import { RepositorioCotizaciones } from '../dominio/repositorios/repositorio-cotizaciones';
import { GestionComercial } from './gestion-comercial';
import { GestionCotizaciones } from './gestion-cotizaciones';

describe('casos de uso comerciales', () => {
  const clientes = { guardar: vi.fn(() => of({})) };
  const catalogos = { guardarLugar: vi.fn(() => of({})) };
  const cotizaciones = { guardar: vi.fn(() => of({})), rechazar: vi.fn(() => of({})) };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: RepositorioClientes, useValue: clientes },
        { provide: RepositorioCatalogosComerciales, useValue: catalogos },
        { provide: RepositorioCotizaciones, useValue: cotizaciones },
      ],
    });
  });

  it('normaliza el cliente y descarta contactos vacios', () => {
    TestBed.inject(GestionComercial)
      .guardarCliente(null, {
        idTipoCliente: 2,
        razonSocial: '  Banco Andino  ',
        direccion: '  ',
        documentos: [{ id: null, idTipoDocumento: 2, numero: ' 1020304025 ' }],
        contactos: [
          { id: null, idTipoContacto: 1, valor: ' 71524488 ', referencia: ' ' },
          { id: null, idTipoContacto: 3, valor: '   ', referencia: null },
        ],
      })
      .subscribe();

    expect(clientes.guardar).toHaveBeenCalledWith(null, {
      idTipoCliente: 2,
      razonSocial: 'Banco Andino',
      direccion: null,
      documentos: [{ id: null, idTipoDocumento: 2, numero: '1020304025' }],
      contactos: [{ id: null, idTipoContacto: 1, valor: '71524488', referencia: null }],
    });
  });

  it('la ciudad vacia del lugar se envia como nula', () => {
    TestBed.inject(GestionComercial).guardarLugar(4, { nombre: ' Coliseo ', direccion: ' Av. Saavedra ', ciudad: '' }).subscribe();

    expect(catalogos.guardarLugar).toHaveBeenCalledWith(4, { nombre: 'Coliseo', direccion: 'Av. Saavedra', ciudad: null });
  });

  it('recorta el titulo, las condiciones y el motivo de rechazo', () => {
    const gestion = TestBed.inject(GestionCotizaciones);
    gestion
      .guardar(null, {
        idCliente: 1,
        titulo: ' Gala ',
        fechaEmision: '2026-11-09',
        fechaValidez: '2026-11-24',
        fechaEventoInicio: null,
        fechaEventoFin: null,
        condiciones: '   ',
        lineas: [],
      })
      .subscribe();
    gestion.rechazar(7, '  Presupuesto insuficiente ').subscribe();

    expect(cotizaciones.guardar).toHaveBeenCalledWith(null, expect.objectContaining({ titulo: 'Gala', condiciones: null }));
    expect(cotizaciones.rechazar).toHaveBeenCalledWith(7, 'Presupuesto insuficiente');
  });
});
