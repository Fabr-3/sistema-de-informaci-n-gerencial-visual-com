import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { guardarRecurso } from '../../../../compartido/utilidades/guardar-recurso';
import { parametrosConsulta } from '../../../../compartido/utilidades/parametros-http';
import { URL_API } from '../../../../nucleo/configuracion/api';
import { DatosLugar, DatosServicio, ElementoCatalogo, Lugar, Servicio } from '../../dominio/modelos/catalogos';
import { RepositorioCatalogosComerciales } from '../../dominio/repositorios/repositorio-catalogos-comerciales';

@Injectable()
export class RepositorioCatalogosComercialesHttp extends RepositorioCatalogosComerciales {
  private readonly http = inject(HttpClient);
  private readonly url = inject(URL_API) + '/comercial/catalogos';

  listarTiposCliente(): Observable<ElementoCatalogo[]> {
    return this.http.get<ElementoCatalogo[]>(`${this.url}/tipos-cliente`);
  }

  listarTiposDocumento(): Observable<ElementoCatalogo[]> {
    return this.http.get<ElementoCatalogo[]>(`${this.url}/tipos-documento`);
  }

  listarTiposContacto(): Observable<ElementoCatalogo[]> {
    return this.http.get<ElementoCatalogo[]>(`${this.url}/tipos-contacto`);
  }

  listarServicios(soloActivos: boolean): Observable<Servicio[]> {
    return this.http.get<Servicio[]>(`${this.url}/servicios`, { params: parametrosConsulta({ soloActivos }) });
  }

  guardarServicio(id: number | null, datos: DatosServicio): Observable<Servicio> {
    return guardarRecurso<Servicio>(this.http, `${this.url}/servicios`, id, datos);
  }

  listarLugares(): Observable<Lugar[]> {
    return this.http.get<Lugar[]>(`${this.url}/lugares`);
  }

  guardarLugar(id: number | null, datos: DatosLugar): Observable<Lugar> {
    return guardarRecurso<Lugar>(this.http, `${this.url}/lugares`, id, datos);
  }
}
