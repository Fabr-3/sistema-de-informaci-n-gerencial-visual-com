import { ActivoReservable, alternarSeleccion, categoriasDe } from './reservas';

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
});
