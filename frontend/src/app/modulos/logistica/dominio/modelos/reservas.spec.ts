import {
  ActivoReservable,
  alternarSeleccion,
  categoriasDe,
  faltantesDe,
  pedidosReservables,
} from './reservas';

const activo = (id: number, categoria: string): ActivoReservable => ({
  id,
  codigo: `ABPL-000${id}`,
  modelo: 'Absen PL3.9',
  categoria,
});

describe('reservas de equipos', () => {
  it('alterna un equipo en la seleccion sin modificar la original', () => {
    const original = new Set([1, 2]);

    expect([...alternarSeleccion(original, 3)]).toEqual([1, 2, 3]);
    expect([...alternarSeleccion(original, 1)]).toEqual([2]);
    expect([...original]).toEqual([1, 2]);
  });

  it('lista las categorias sin repetir y ordenadas', () => {
    expect(categoriasDe([activo(1, 'Sonido'), activo(2, 'Iluminación'), activo(3, 'Sonido')])).toEqual([
      'Iluminación',
      'Sonido',
    ]);
  });

  it('calcula los faltantes y lo que se puede reservar de cada modelo', () => {
    const disponibilidad = [
      { idModelo: 1, modelo: 'Robe Pointe', categoria: 'Iluminación', disponibles: 4 },
      { idModelo: 2, modelo: 'Shure SM58', categoria: 'Audio', disponibles: 0 },
    ];
    const pedidos = [
      { idModelo: 1, cantidad: 6 },
      { idModelo: 2, cantidad: 2 },
      { idModelo: 3, cantidad: 1 },
    ];

    expect(faltantesDe(pedidos, disponibilidad)).toEqual([
      { idModelo: 1, modelo: 'Robe Pointe', solicitada: 6, disponibles: 4, faltante: 2 },
      { idModelo: 2, modelo: 'Shure SM58', solicitada: 2, disponibles: 0, faltante: 2 },
      { idModelo: 3, modelo: '', solicitada: 1, disponibles: 0, faltante: 1 },
    ]);
    expect(pedidosReservables(pedidos, disponibilidad)).toEqual([{ idModelo: 1, cantidad: 4 }]);
  });
});
