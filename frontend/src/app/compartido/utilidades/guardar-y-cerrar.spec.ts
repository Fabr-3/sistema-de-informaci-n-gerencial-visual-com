import { signal } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';

import { guardarYCerrar } from './guardar-y-cerrar';

describe('guardarYCerrar', () => {
  const dialogo = { close: vi.fn() } as unknown as MatDialogRef<unknown, string>;

  it('cierra el dialogo con el resultado', () => {
    const guardando = signal(false);

    guardarYCerrar(of('guardado'), dialogo, guardando, vi.fn());

    expect(dialogo.close).toHaveBeenCalledWith('guardado');
  });

  it('informa el error y permite reintentar', () => {
    const guardando = signal(false);
    const alFallar = vi.fn();

    guardarYCerrar(throwError(() => new Error('409')), dialogo, guardando, alFallar);

    expect(alFallar).toHaveBeenCalled();
    expect(guardando()).toBe(false);
  });
});
