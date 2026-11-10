import { separarServicios } from './proveedor';

describe('proveedores', () => {
  it('separa los servicios por comas o lineas y descarta los vacios', () => {
    expect(separarServicios(' Transporte de carga, grúa \n\n Generador eléctrico,')).toEqual([
      'Transporte de carga',
      'grúa',
      'Generador eléctrico',
    ]);
  });
});
