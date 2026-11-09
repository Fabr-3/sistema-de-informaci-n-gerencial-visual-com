import { FormControl, FormGroup } from '@angular/forms';

import { camposIguales, politicaContrasena } from './validadores-contrasena';

describe('validadores de contrasena', () => {
  it.each(['admin123456', 'ñandu2026'])('acepta %s', (valor) => {
    expect(politicaContrasena(new FormControl(valor))).toBeNull();
  });

  it.each(['corta1', 'solamenteletras', '1234567890', 'a1'.repeat(37)])('rechaza %s', (valor) => {
    expect(politicaContrasena(new FormControl(valor))).toEqual({ politicaContrasena: true });
  });

  it('deja la obligatoriedad al validador required', () => {
    expect(politicaContrasena(new FormControl(''))).toBeNull();
  });

  it('detecta cuando la confirmacion no coincide', () => {
    const grupo = new FormGroup(
      { nueva: new FormControl('clave12345'), confirmacion: new FormControl('clave12346') },
      { validators: camposIguales('nueva', 'confirmacion') },
    );
    expect(grupo.hasError('noCoinciden')).toBe(true);

    grupo.controls.confirmacion.setValue('clave12345');
    expect(grupo.hasError('noCoinciden')).toBe(false);
  });
});
