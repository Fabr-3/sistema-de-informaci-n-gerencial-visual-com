import { TestBed } from '@angular/core/testing';
import { Observable, Subject, firstValueFrom, of, throwError } from 'rxjs';

import { CambioContrasena, Credenciales, Sesion } from '../dominio/modelos/sesion';
import { RepositorioAutenticacion } from '../dominio/repositorios/repositorio-autenticacion';
import { sesion } from '../pruebas/datos-prueba';
import { ServicioSesion } from './servicio-sesion';

class RepositorioAutenticacionFalso extends RepositorioAutenticacion {
  respuestaRefresco: Observable<Sesion> = of(sesion('token-refrescado'));
  llamadasRefresco = 0;
  cerroSesion = false;

  iniciarSesion(credenciales: Credenciales): Observable<Sesion> {
    return credenciales.contrasena === 'correcta1' ? of(sesion()) : throwError(() => new Error('401'));
  }

  refrescar(): Observable<Sesion> {
    this.llamadasRefresco++;
    return this.respuestaRefresco;
  }

  cerrarSesion(): Observable<void> {
    this.cerroSesion = true;
    return of(undefined);
  }

  cambiarContrasena(_cambio: CambioContrasena): Observable<Sesion> {
    return of(sesion('token-nuevo', { debeCambiarContrasena: false }));
  }
}

describe('ServicioSesion', () => {
  let servicio: ServicioSesion;
  let repositorio: RepositorioAutenticacionFalso;

  beforeEach(() => {
    repositorio = new RepositorioAutenticacionFalso();
    TestBed.configureTestingModule({ providers: [{ provide: RepositorioAutenticacion, useValue: repositorio }] });
    servicio = TestBed.inject(ServicioSesion);
  });

  it('inicia sin sesion', () => {
    expect(servicio.estaAutenticado()).toBe(false);
    expect(servicio.tokenAcceso()).toBeNull();
    expect(servicio.tienePermiso('USUARIOS_VER')).toBe(false);
  });

  it('guarda la sesion y los permisos al iniciar sesion', async () => {
    await firstValueFrom(servicio.iniciarSesion({ nombreUsuario: 'admin', contrasena: 'correcta1' }));

    expect(servicio.estaAutenticado()).toBe(true);
    expect(servicio.tokenAcceso()).toBe('token-1');
    expect(servicio.tienePermiso('USUARIOS_GESTIONAR')).toBe(true);
    expect(servicio.tienePermiso('AUDITORIA_VER')).toBe(false);
  });

  it('comparte un unico refresco entre solicitudes simultaneas', async () => {
    const respuesta = new Subject<Sesion>();
    repositorio.respuestaRefresco = respuesta;

    const primera = firstValueFrom(servicio.refrescar());
    const segunda = firstValueFrom(servicio.refrescar());
    respuesta.next(sesion('token-refrescado'));
    respuesta.complete();

    expect((await primera).tokenAcceso).toBe('token-refrescado');
    expect((await segunda).tokenAcceso).toBe('token-refrescado');
    expect(repositorio.llamadasRefresco).toBe(1);
  });

  it('restaurar no falla si no hay cookie de refresco valida', async () => {
    repositorio.respuestaRefresco = throwError(() => new Error('401'));

    await expect(servicio.restaurar()).resolves.toBeUndefined();
    expect(servicio.estaAutenticado()).toBe(false);
  });

  it('cerrar sesion limpia el estado local', async () => {
    await firstValueFrom(servicio.iniciarSesion({ nombreUsuario: 'admin', contrasena: 'correcta1' }));

    await firstValueFrom(servicio.cerrarSesion(), { defaultValue: undefined });

    expect(repositorio.cerroSesion).toBe(true);
    expect(servicio.estaAutenticado()).toBe(false);
  });
});
