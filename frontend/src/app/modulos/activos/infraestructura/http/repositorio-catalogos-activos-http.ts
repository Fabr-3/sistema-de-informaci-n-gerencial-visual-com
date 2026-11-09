import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { guardarRecurso } from '../../../../compartido/utilidades/guardar-recurso';
import { parametrosConsulta } from '../../../../compartido/utilidades/parametros-http';
import { URL_API } from '../../../../nucleo/configuracion/api';
import { Bodega, Categoria, DatosBodega, Fabricante, TipoMantenimiento } from '../../dominio/modelos/catalogos';
import { DatosModelo, ModeloActivo, TipoControl } from '../../dominio/modelos/modelo-activo';
import { RepositorioCatalogosActivos } from '../../dominio/repositorios/repositorio-catalogos-activos';

@Injectable()
export class RepositorioCatalogosActivosHttp extends RepositorioCatalogosActivos {
  private readonly http = inject(HttpClient);
  private readonly urlCatalogos = inject(URL_API) + '/activos/catalogos';
  private readonly urlModelos = inject(URL_API) + '/activos/modelos';

  listarCategorias(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(`${this.urlCatalogos}/categorias`);
  }

  guardarCategoria(id: number | null, nombre: string): Observable<Categoria> {
    return guardarRecurso<Categoria>(this.http, `${this.urlCatalogos}/categorias`, id, { nombre });
  }

  listarFabricantes(): Observable<Fabricante[]> {
    return this.http.get<Fabricante[]>(`${this.urlCatalogos}/fabricantes`);
  }

  guardarFabricante(id: number | null, nombre: string): Observable<Fabricante> {
    return guardarRecurso<Fabricante>(this.http, `${this.urlCatalogos}/fabricantes`, id, { nombre });
  }

  listarBodegas(): Observable<Bodega[]> {
    return this.http.get<Bodega[]>(`${this.urlCatalogos}/bodegas`);
  }

  guardarBodega(id: number | null, datos: DatosBodega): Observable<Bodega> {
    return guardarRecurso<Bodega>(this.http, `${this.urlCatalogos}/bodegas`, id, datos);
  }

  listarTiposMantenimiento(): Observable<TipoMantenimiento[]> {
    return this.http.get<TipoMantenimiento[]>(`${this.urlCatalogos}/tipos-mantenimiento`);
  }

  listarModelos(tipoControl?: TipoControl): Observable<ModeloActivo[]> {
    return this.http.get<ModeloActivo[]>(this.urlModelos, { params: parametrosConsulta({ tipoControl }) });
  }

  guardarModelo(id: number | null, datos: DatosModelo): Observable<ModeloActivo> {
    return guardarRecurso<ModeloActivo>(this.http, this.urlModelos, id, datos);
  }
}
