import { FormControl } from '@angular/forms';

import { requiereSeleccion, seleccionOpcional } from './validadores-seleccion';

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

  it('el campo opcional admite vacio pero no texto sin elegir', () => {
    const validarOpcional = (valor: unknown) => seleccionOpcional(new FormControl(valor));
    expect(validarOpcional(null)).toBeNull();
    expect(validarOpcional('')).toBeNull();
    expect(validarOpcional({ id: 2, nombre: 'Proyecto' })).toBeNull();
    expect(validarOpcional('Proy')).toEqual({ seleccionRequerida: true });
  });
});
