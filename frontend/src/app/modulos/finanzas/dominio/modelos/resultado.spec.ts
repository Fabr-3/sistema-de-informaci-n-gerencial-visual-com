import { Margen, textoPorcentaje, tipoVisualMargen } from './resultado';

describe('resultados por proyecto', () => {
  const margen = (porcentaje: number | null, costos = 100): Margen => ({ ingresos: 1000, costos, margen: 0, porcentaje });

  it('clasifica el margen: negativo, bajo, saludable o sin ingresos', () => {
    expect(tipoVisualMargen(margen(-12.5))).toBe('peligro');
    expect(tipoVisualMargen(margen(15))).toBe('alerta');
    expect(tipoVisualMargen(margen(68.1))).toBe('activo');
    expect(tipoVisualMargen(margen(null))).toBe('neutro');
  });

  it('muestra el porcentaje o la falta de ingresos', () => {
    expect(textoPorcentaje(margen(68.1))).toBe('68,1 %');
    expect(textoPorcentaje(margen(null))).toBe('Sin ingresos');
  });
});
