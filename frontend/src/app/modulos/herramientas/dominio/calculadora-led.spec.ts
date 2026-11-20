import { EntradaCalculadoraLed, calcularPantallaLed } from './calculadora-led';

const base: EntradaCalculadoraLed = {
  modo: 'dimensiones',
  ancho: 6,
  alto: 3.4,
  area: null,
  pitchMm: 3.9,
  moduloAnchoMm: 500,
  moduloAltoMm: 500,
  precioM2Dia: 150,
  dias: 2,
};

describe('calculadora de pantallas LED', () => {
  it('arma la pantalla con modulos completos y calcula resolucion y costo', () => {
    const resultado = calcularPantallaLed(base)!;

    expect(resultado.columnas).toBe(12);
    expect(resultado.filas).toBe(7);
    expect(resultado.modulos).toBe(84);
    expect(resultado.altoArmado).toBeCloseTo(3.5);
    expect(resultado.areaArmada).toBeCloseTo(21);
    expect(resultado.pixelesAncho).toBe(1538);
    expect(resultado.pixelesAlto).toBe(897);
    expect(resultado.costoPorDia).toBeCloseTo(3150);
    expect(resultado.costoTotal).toBeCloseTo(6300);
    expect(resultado.distanciaMinimaVision).toBe(3.9);
  });

  it('con solo el area supone una pantalla 16:9', () => {
    const resultado = calcularPantallaLed({ ...base, modo: 'area', ancho: null, alto: null, area: 9 })!;

    expect(resultado.anchoPedido / resultado.altoPedido).toBeCloseTo(16 / 9);
    expect(resultado.areaPedida).toBeCloseTo(9);
  });

  it('no calcula mientras falten datos o haya valores no positivos', () => {
    expect(calcularPantallaLed({ ...base, alto: null })).toBeNull();
    expect(calcularPantallaLed({ ...base, pitchMm: 0 })).toBeNull();
    expect(calcularPantallaLed({ ...base, dias: -1 })).toBeNull();
    expect(calcularPantallaLed({ ...base, modo: 'area', area: null })).toBeNull();
  });
});
