import { combinarFechaHora, separarFechaHora } from './fecha-hora';

describe('fecha y hora de los selectores', () => {
  it('une la fecha y la hora en el formato de la API', () => {
    expect(combinarFechaHora(new Date(2026, 11, 11), new Date(2000, 0, 1, 8, 5))).toBe('2026-12-11T08:05');
    expect(combinarFechaHora(new Date(2026, 11, 11), null)).toBe('');
    expect(combinarFechaHora(new Date(Number.NaN), new Date())).toBe('');
  });

  it('separa el texto en fecha y hora locales', () => {
    const { fecha, hora } = separarFechaHora('2026-12-11T14:30:00');

    expect(fecha).toEqual(new Date(2026, 11, 11));
    expect(hora?.getHours()).toBe(14);
    expect(hora?.getMinutes()).toBe(30);
    expect(separarFechaHora('')).toEqual({ fecha: null, hora: null });
  });
});
