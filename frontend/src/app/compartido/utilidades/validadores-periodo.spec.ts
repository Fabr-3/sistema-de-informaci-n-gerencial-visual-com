import { FormControl, FormGroup } from '@angular/forms';

import { finPosteriorAInicio, noFutura } from './validadores-periodo';

describe('validadores de periodos', () => {
  it('exige que el fin sea posterior al inicio', () => {
    const grupo = new FormGroup(
      { inicio: new FormControl('2026-11-01T10:00'), fin: new FormControl('2026-11-01T09:00') },
      { validators: finPosteriorAInicio('inicio', 'fin') },
    );
    expect(grupo.hasError('periodoInvalido')).toBe(true);

    grupo.controls.fin.setValue('2026-11-01T11:00');
    expect(grupo.hasError('periodoInvalido')).toBe(false);
  });

  it('rechaza fechas futuras', () => {
    expect(noFutura(new FormControl('2999-01-01T00:00'))).toEqual({ fechaFutura: true });
    expect(noFutura(new FormControl('2020-01-01T00:00'))).toBeNull();
    expect(noFutura(new FormControl(''))).toBeNull();
  });
});
