import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { URL_API } from '../../../../nucleo/configuracion/api';
import { CambioContrasena, Credenciales, Sesion, UsuarioSesion } from '../../dominio/modelos/sesion';
import { RepositorioAutenticacion } from '../../dominio/repositorios/repositorio-autenticacion';

/** Respuesta de la API: el token de refresco llega solo como cookie HttpOnly. */
interface RespuestaSesion {
  readonly tokenAcceso: string;
  readonly expiraEn: string;
  readonly usuario: UsuarioSesion;
}

export const RUTA_AUTENTICACION = '/auth';
export const RUTAS_PUBLICAS_AUTENTICACION = ['/inicio-sesion', '/refresco', '/cierre-sesion'].map(
  (ruta) => RUTA_AUTENTICACION + ruta,
);

@Injectable()
export class RepositorioAutenticacionHttp extends RepositorioAutenticacion {
  private readonly http = inject(HttpClient);
  private readonly url = inject(URL_API) + RUTA_AUTENTICACION;

  iniciarSesion(credenciales: Credenciales): Observable<Sesion> {
    return this.http.post<RespuestaSesion>(`${this.url}/inicio-sesion`, credenciales).pipe(map(aSesion));
  }

  refrescar(): Observable<Sesion> {
    return this.http.post<RespuestaSesion>(`${this.url}/refresco`, null).pipe(map(aSesion));
  }

  cerrarSesion(): Observable<void> {
    return this.http.post<void>(`${this.url}/cierre-sesion`, null);
  }

  cambiarContrasena(cambio: CambioContrasena): Observable<Sesion> {
    return this.http.put<RespuestaSesion>(`${this.url}/contrasena`, cambio).pipe(map(aSesion));
  }
}

function aSesion(respuesta: RespuestaSesion): Sesion {
  return { tokenAcceso: respuesta.tokenAcceso, expiraEn: new Date(respuesta.expiraEn), usuario: respuesta.usuario };
}
