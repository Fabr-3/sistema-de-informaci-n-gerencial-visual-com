import { FormControl } from '@angular/forms';

import { requiereSeleccion } from './validadores-seleccion';

describe('requiereSeleccion', () => {
  const validar = (valor: unknown) => requiereSeleccion(new FormControl(valor));

  it('acepta un elemento elegido de la lista', () => {
    expect(validar({ id: 4, razonSocial: 'Banco Andino de Desarrollo S.A.' })).toBeNull();
  });

  it('rechaza el texto escrito sin elegir y el valor vacio', () => {
    expect(validar('Banco')).toEqual({ seleccionRequerida: true });
    expect(validar(null)).toEqual({ seleccionRequerida: true });
    expect(validar({ razonSocial: 'Sin id' })).toEqual({ seleccionRequerida: true });
  });
});
