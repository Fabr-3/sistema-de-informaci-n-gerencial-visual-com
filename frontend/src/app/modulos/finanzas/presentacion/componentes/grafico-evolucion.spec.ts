import { maximoRedondo, trazoBarra } from './grafico-evolucion';

describe('grafico de evolucion', () => {
  it('redondea el maximo del eje al paso mas cercano por potencia de diez', () => {
    expect(maximoRedondo(57000)).toBe(60000);
    expect(maximoRedondo(47700)).toBe(50000);
    expect(maximoRedondo(21000)).toBe(24000);
    expect(maximoRedondo(1800)).toBe(2000);
    expect(maximoRedondo(0)).toBe(1);
  });

  it('dibuja la barra con el tope redondeado y omite las vacias', () => {
    expect(trazoBarra(10, 18, 200, 0)).toBe('');
    expect(trazoBarra(10, 18, 200, 100)).toBe('M10,200 V104 Q10,100 14,100 H24 Q28,100 28,104 V200 Z');
  });
});
