import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, firstValueFrom, map, of, shareReplay, tap, throwError } from 'rxjs';

import { CambioContrasena, Credenciales, Sesion, UsuarioSesion } from '../dominio/modelos/sesion';
import { RepositorioAutenticacion } from '../dominio/repositorios/repositorio-autenticacion';

/**
 * Estado de la sesion del usuario. Es la unica fuente de verdad sobre quien
 * esta autenticado y que permisos tiene.
 */
@Injectable({ providedIn: 'root' })
export class ServicioSesion {
  private readonly repositorio = inject(RepositorioAutenticacion);
  private readonly sesion = signal<Sesion | null>(null);
  /** Refresco compartido: evita rotar dos veces el token con solicitudes simultaneas. */
  private refrescoEnCurso: Observable<Sesion> | null = null;

  readonly usuario = computed<UsuarioSesion | null>(() => this.sesion()?.usuario ?? null);
  readonly estaAutenticado = computed(() => this.sesion() !== null);
  readonly debeCambiarContrasena = computed(() => this.usuario()?.debeCambiarContrasena ?? false);

  tokenAcceso(): string | null {
    return this.sesion()?.tokenAcceso ?? null;
  }

  tienePermiso(permiso: string): boolean {
    return this.usuario()?.permisos.includes(permiso) ?? false;
  }

  iniciarSesion(credenciales: Credenciales): Observable<Sesion> {
    return this.repositorio.iniciarSesion(credenciales).pipe(tap((sesion) => this.sesion.set(sesion)));
  }

  /** Renueva el token de acceso usando la cookie de refresco. */
  refrescar(): Observable<Sesion> {
    this.refrescoEnCurso ??= this.repositorio.refrescar().pipe(
      tap((sesion) => this.sesion.set(sesion)),
      catchError((error: unknown) => {
        this.sesion.set(null);
        return throwError(() => error);
      }),
      finalize(() => (this.refrescoEnCurso = null)),
      shareReplay(1),
    );
    return this.refrescoEnCurso;
  }

  /** Intenta recuperar la sesion al cargar la aplicacion; sin cookie valida queda sin sesion. */
  restaurar(): Promise<void> {
    return firstValueFrom(
      this.refrescar().pipe(
        map(() => undefined),
        catchError(() => of(undefined)),
      ),
    );
  }

  /** Cierra la sesion en el servidor; localmente se cierra aunque la llamada falle. */
  cerrarSesion(): Observable<void> {
    return this.repositorio.cerrarSesion().pipe(
      catchError(() => of(undefined)),
      finalize(() => this.finalizarLocalmente()),
    );
  }

  cambiarContrasena(cambio: CambioContrasena): Observable<Sesion> {
    return this.repositorio.cambiarContrasena(cambio).pipe(tap((sesion) => this.sesion.set(sesion)));
  }

  finalizarLocalmente(): void {
    this.sesion.set(null);
  }
}
