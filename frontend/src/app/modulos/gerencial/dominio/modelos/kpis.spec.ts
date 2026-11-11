import { TasaUtilizacion, anchoBarra, cambioEnPuntos } from './kpis';

describe('KPIs gerenciales', () => {
  it('expresa el cambio de un margen en puntos porcentuales', () => {
    expect(cambioEnPuntos(37.1, 63)).toEqual({ puntos: 25.9, sentido: 'baja' });
    expect(cambioEnPuntos(45, 40.5)).toEqual({ puntos: 4.5, sentido: 'sube' });
    expect(cambioEnPuntos(20, 20)).toEqual({ puntos: 0, sentido: 'igual' });
    expect(cambioEnPuntos(null, 20)).toBeNull();
  });

  it('limita el ancho de la barra de utilizacion entre 0 y 100', () => {
    const tasa = (porcentaje: number | null): TasaUtilizacion => ({
      recursos: 1,
      diasDisponibles: 30,
      diasOcupados: 3,
      porcentaje,
    });
    expect(anchoBarra(tasa(31.9))).toBe(31.9);
    expect(anchoBarra(tasa(null))).toBe(0);
    expect(anchoBarra(tasa(120))).toBe(100);
  });
});
