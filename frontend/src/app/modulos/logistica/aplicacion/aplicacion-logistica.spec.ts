import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { RepositorioPersonal } from '../dominio/repositorios/repositorio-personal';
import { RepositorioPlanificacion } from '../dominio/repositorios/repositorio-planificacion';
import { GestionPersonal } from './gestion-personal';
import { GestionPlanificacion } from './gestion-planificacion';

describe('casos de uso de logistica', () => {
  const personal = { guardar: vi.fn(() => of({})) };
  const planificacion = {
    listar: vi.fn(() => of({})),
    guardarJornada: vi.fn(() => of({})),
    guardarTransporte: vi.fn(() => of({})),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: RepositorioPersonal, useValue: personal },
        { provide: RepositorioPlanificacion, useValue: planificacion },
      ],
    });
  });

  it('normaliza los datos del personal y envia el telefono vacio como nulo', () => {
    TestBed.inject(GestionPersonal)
      .guardar(null, {
        nombres: ' Juan Carlos ',
        apellidos: ' Mamani Quispe ',
        telefono: '  ',
        idTipoDocumento: 1,
        numeroDocumento: ' 4876512 LP ',
        idsEspecialidades: [1, 3],
        idUsuario: null,
        activo: true,
      })
      .subscribe();

    expect(personal.guardar).toHaveBeenCalledWith(null, {
      nombres: 'Juan Carlos',
      apellidos: 'Mamani Quispe',
      telefono: null,
      idTipoDocumento: 1,
      numeroDocumento: '4876512 LP',
      idsEspecialidades: [1, 3],
      idUsuario: null,
      activo: true,
    });
  });

  it('recorta la busqueda, la descripcion de la jornada y el transporte', () => {
    const gestion = TestBed.inject(GestionPlanificacion);
    gestion.listar({ texto: '   ', desde: '2026-11-09' }, { pagina: 0, tamano: 20 }).subscribe();
    gestion
      .guardarJornada(7, null, { tipo: 'MONTAJE', inicio: '2026-12-05T08:00', fin: '2026-12-05T17:00', descripcion: ' ' })
      .subscribe();
    gestion.guardarTransporte(7, ' Camión 1 desde Bodega 1 ').subscribe();

    expect(planificacion.listar).toHaveBeenCalledWith({ texto: undefined, desde: '2026-11-09' }, { pagina: 0, tamano: 20 });
    expect(planificacion.guardarJornada).toHaveBeenCalledWith(7, null, {
      tipo: 'MONTAJE',
      inicio: '2026-12-05T08:00',
      fin: '2026-12-05T17:00',
      descripcion: null,
    });
    expect(planificacion.guardarTransporte).toHaveBeenCalledWith(7, 'Camión 1 desde Bodega 1');
  });
});
