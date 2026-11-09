import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { parametrosConsulta } from '../../../../compartido/utilidades/parametros-http';
import { URL_API } from '../../../../nucleo/configuracion/api';
import {
  EstadoModuloLed,
  FinMantenimiento,
  InicioMantenimiento,
  Mantenimiento,
  ModuloLed,
  NuevoRegistroUso,
  RegistroUso,
} from '../../dominio/modelos/historial';
import { RepositorioHistorialActivos } from '../../dominio/repositorios/repositorio-historial-activos';

@Injectable()
export class RepositorioHistorialActivosHttp extends RepositorioHistorialActivos {
  private readonly http = inject(HttpClient);
  private readonly url = inject(URL_API) + '/activos';

  listarUsos(idActivo: number, solicitud: SolicitudPagina): Observable<Pagina<RegistroUso>> {
    return this.http.get<Pagina<RegistroUso>>(`${this.url}/${idActivo}/usos`, {
      params: parametrosConsulta({ ...solicitud }),
    });
  }

  registrarUso(idActivo: number, uso: NuevoRegistroUso): Observable<RegistroUso> {
    return this.http.post<RegistroUso>(`${this.url}/${idActivo}/usos`, uso);
  }

  anularUso(idActivo: number, idRegistro: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${idActivo}/usos/${idRegistro}`);
  }

  listarMantenimientos(idActivo: number, solicitud: SolicitudPagina): Observable<Pagina<Mantenimiento>> {
    return this.http.get<Pagina<Mantenimiento>>(`${this.url}/${idActivo}/mantenimientos`, {
      params: parametrosConsulta({ ...solicitud }),
    });
  }

  iniciarMantenimiento(idActivo: number, inicio: InicioMantenimiento): Observable<Mantenimiento> {
    return this.http.post<Mantenimiento>(`${this.url}/${idActivo}/mantenimientos`, inicio);
  }

  finalizarMantenimiento(idActivo: number, idMantenimiento: number, fin: FinMantenimiento): Observable<Mantenimiento> {
    return this.http.post<Mantenimiento>(
      `${this.url}/${idActivo}/mantenimientos/${idMantenimiento}/finalizacion`,
      fin,
    );
  }

  listarModulos(idActivo: number): Observable<ModuloLed[]> {
    return this.http.get<ModuloLed[]>(`${this.url}/${idActivo}/modulos`);
  }

  agregarModulo(idActivo: number, codigo: string): Observable<ModuloLed> {
    return this.http.post<ModuloLed>(`${this.url}/${idActivo}/modulos`, { codigo });
  }

  cambiarEstadoModulo(idActivo: number, idModulo: number, estado: EstadoModuloLed): Observable<ModuloLed> {
    return this.http.patch<ModuloLed>(`${this.url}/${idActivo}/modulos/${idModulo}/estado`, { estado });
  }
}
