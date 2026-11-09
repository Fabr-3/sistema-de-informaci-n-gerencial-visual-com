import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';

import { ServicioSesion } from '../../../aplicacion/servicio-sesion';
import { sesion } from '../../../pruebas/datos-prueba';
import { PaginaInicioSesion } from './pagina-inicio-sesion';

describe('PaginaInicioSesion', () => {
  const servicioSesion = { iniciarSesion: vi.fn() };
  const router = { navigateByUrl: vi.fn() };
  let retorno: string | null = null;

  beforeEach(async () => {
    vi.clearAllMocks();
    retorno = null;
    await TestBed.configureTestingModule({
      imports: [PaginaInicioSesion],
      providers: [
        { provide: ServicioSesion, useValue: servicioSesion },
        { provide: Router, useValue: router },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { get queryParamMap() { return convertToParamMap(retorno ? { retorno } : {}); } } },
        },
      ],
    }).compileComponents();
  });

  async function enviar(contrasena = 'admin123456'): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(PaginaInicioSesion);
    fixture.detectChanges();
    const elemento = fixture.nativeElement as HTMLElement;
    escribir(elemento, 'input[formcontrolname="nombreUsuario"]', 'admin');
    escribir(elemento, 'input[formcontrolname="contrasena"]', contrasena);
    elemento.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    return elemento;
  }

  it('al iniciar sesion navega a la ruta de retorno interna', async () => {
    retorno = '/accesos/usuarios';
    servicioSesion.iniciarSesion.mockReturnValue(of(sesion()));

    await enviar();

    expect(servicioSesion.iniciarSesion).toHaveBeenCalledWith({ nombreUsuario: 'admin', contrasena: 'admin123456' });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/accesos/usuarios');
  });

  it('ignora rutas de retorno externas', async () => {
    retorno = '//sitio-malicioso.com';
    servicioSesion.iniciarSesion.mockReturnValue(of(sesion()));

    await enviar();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/');
  });

  it('con contrasena temporal envia a cambiarla', async () => {
    servicioSesion.iniciarSesion.mockReturnValue(of(sesion('token', { debeCambiarContrasena: true })));

    await enviar();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/cambio-contrasena');
  });

  it('muestra el mensaje de la API si las credenciales son invalidas', async () => {
    servicioSesion.iniciarSesion.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 401, error: { detail: 'Usuario o contrasena incorrectos' } })),
    );

    const elemento = await enviar('incorrecta1');

    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain('Usuario o contrasena incorrectos');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });
});

function escribir(elemento: HTMLElement, selector: string, valor: string): void {
  const campo = elemento.querySelector<HTMLInputElement>(selector)!;
  campo.value = valor;
  campo.dispatchEvent(new Event('input'));
}
