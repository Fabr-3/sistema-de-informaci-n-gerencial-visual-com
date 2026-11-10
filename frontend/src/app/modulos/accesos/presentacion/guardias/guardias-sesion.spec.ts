import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';

import { ServicioSesion } from '../../aplicacion/servicio-sesion';
import { autenticacionGuard, invitadoGuard, permisoGuard } from './guardias-sesion';

describe('guardias de sesion', () => {
  const sesion = {
    estaAutenticado: vi.fn(() => false),
    debeCambiarContrasena: vi.fn(() => false),
    tienePermiso: vi.fn((_permiso: string) => false),
    tieneAlgunPermiso: vi.fn((...permisos: string[]) => permisos.some((permiso) => sesion.tienePermiso(permiso))),
  };
  const ruta = {} as ActivatedRouteSnapshot;
  const estado = { url: '/accesos/usuarios' } as RouterStateSnapshot;

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: ServicioSesion, useValue: sesion }],
    });
  });

  const ejecutar = (guardia: typeof autenticacionGuard) =>
    TestBed.runInInjectionContext(() => guardia(ruta, estado));
  const url = (resultado: unknown) => TestBed.inject(Router).serializeUrl(resultado as UrlTree);

  it('sin sesion redirige al inicio de sesion conservando la ruta de retorno', () => {
    expect(url(ejecutar(autenticacionGuard))).toBe('/inicio-sesion?retorno=%2Faccesos%2Fusuarios');
  });

  it('con contrasena temporal obliga a cambiarla', () => {
    sesion.estaAutenticado.mockReturnValue(true);
    sesion.debeCambiarContrasena.mockReturnValue(true);

    expect(url(ejecutar(autenticacionGuard))).toBe('/cambio-contrasena');
  });

  it('permite el acceso con sesion valida', () => {
    sesion.estaAutenticado.mockReturnValue(true);
    sesion.debeCambiarContrasena.mockReturnValue(false);

    expect(ejecutar(autenticacionGuard)).toBe(true);
  });

  it('la guardia de permiso redirige al inicio si falta el permiso', () => {
    expect(url(ejecutar(permisoGuard('AUDITORIA_VER')))).toBe('/');

    sesion.tienePermiso.mockImplementation((permiso: string) => permiso === 'AUDITORIA_VER');
    expect(ejecutar(permisoGuard('AUDITORIA_VER'))).toBe(true);
    expect(ejecutar(permisoGuard('USUARIOS_GESTIONAR', 'AUDITORIA_VER'))).toBe(true);
    expect(url(ejecutar(permisoGuard('USUARIOS_GESTIONAR')))).toBe('/');
  });

  it('un usuario autenticado no ve el inicio de sesion', () => {
    sesion.estaAutenticado.mockReturnValue(true);

    expect(url(ejecutar(invitadoGuard))).toBe('/');
  });
});
