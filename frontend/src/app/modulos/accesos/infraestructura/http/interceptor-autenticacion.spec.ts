import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { ServicioSesion } from '../../aplicacion/servicio-sesion';
import { sesion } from '../../pruebas/datos-prueba';
import { interceptorAutenticacion } from './interceptor-autenticacion';

describe('interceptorAutenticacion', () => {
  let http: HttpClient;
  let servidor: HttpTestingController;
  const sesionFalsa = {
    tokenAcceso: vi.fn(() => 'token-1'),
    refrescar: vi.fn(() => of(sesion('token-2'))),
    finalizarLocalmente: vi.fn(),
  };
  const router = { navigateByUrl: vi.fn() };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([interceptorAutenticacion])),
        provideHttpClientTesting(),
        { provide: ServicioSesion, useValue: sesionFalsa },
        { provide: Router, useValue: router },
      ],
    });
    http = TestBed.inject(HttpClient);
    servidor = TestBed.inject(HttpTestingController);
  });

  afterEach(() => servidor.verify());

  it('agrega el token de acceso a la API', () => {
    http.get('/api/usuarios').subscribe();

    expect(servidor.expectOne('/api/usuarios').request.headers.get('Authorization')).toBe('Bearer token-1');
  });

  it('no envia el token a las rutas publicas de autenticacion ni fuera de la API', () => {
    http.post('/api/auth/refresco', null).subscribe();
    http.get('/assets/datos.json').subscribe();

    expect(servidor.expectOne('/api/auth/refresco').request.headers.has('Authorization')).toBe(false);
    expect(servidor.expectOne('/assets/datos.json').request.headers.has('Authorization')).toBe(false);
  });

  it('ante un 401 refresca la sesion y repite la solicitud con el nuevo token', () => {
    let respuesta: unknown;
    http.get('/api/usuarios').subscribe((datos) => (respuesta = datos));

    servidor.expectOne('/api/usuarios').flush(null, { status: 401, statusText: 'Unauthorized' });
    const reintento = servidor.expectOne('/api/usuarios');
    expect(reintento.request.headers.get('Authorization')).toBe('Bearer token-2');
    reintento.flush({ ok: true });

    expect(respuesta).toEqual({ ok: true });
  });

  it('si el refresco falla cierra la sesion local y redirige al inicio de sesion', () => {
    sesionFalsa.refrescar.mockReturnValueOnce(throwError(() => new Error('sesion vencida')));
    let fallo = false;
    http.get('/api/usuarios').subscribe({ error: () => (fallo = true) });

    servidor.expectOne('/api/usuarios').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(fallo).toBe(true);
    expect(sesionFalsa.finalizarLocalmente).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/inicio-sesion');
  });

  it('no intenta refrescar ante errores distintos de 401', () => {
    http.get('/api/usuarios').subscribe({ error: () => undefined });

    servidor.expectOne('/api/usuarios').flush(null, { status: 403, statusText: 'Forbidden' });

    expect(sesionFalsa.refrescar).not.toHaveBeenCalled();
  });
});
