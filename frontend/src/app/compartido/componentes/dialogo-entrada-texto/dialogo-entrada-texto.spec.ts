import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { DatosEntradaTexto, DialogoEntradaTexto } from './dialogo-entrada-texto';

describe('DialogoEntradaTexto', () => {
  const dialogo = { close: vi.fn() };
  const datos: DatosEntradaTexto = { titulo: 'Nueva categoría', etiqueta: 'Nombre', textoConfirmar: 'Guardar', longitudMaxima: 10 };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [DialogoEntradaTexto],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: datos },
        { provide: MatDialogRef, useValue: dialogo },
      ],
    }).compileComponents();
  });

  function enviar(valor: string): Event {
    const fixture = TestBed.createComponent(DialogoEntradaTexto);
    fixture.detectChanges();
    const elemento = fixture.nativeElement as HTMLElement;
    const campo = elemento.querySelector('input')!;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
    const evento = new Event('submit', { cancelable: true });
    elemento.querySelector('form')!.dispatchEvent(evento);
    return evento;
  }

  it('cierra con el texto recortado e impide el envio nativo del formulario', () => {
    const evento = enviar('  Sonido  ');

    expect(evento.defaultPrevented).toBe(true);
    expect(dialogo.close).toHaveBeenCalledWith('Sonido');
  });

  it('no cierra si el texto esta vacio o solo tiene espacios', () => {
    enviar('   ');

    expect(dialogo.close).not.toHaveBeenCalled();
  });
});
