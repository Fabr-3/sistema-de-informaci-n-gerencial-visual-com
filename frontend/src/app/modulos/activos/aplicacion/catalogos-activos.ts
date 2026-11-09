import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Bodega, Categoria, DatosBodega, Fabricante, TipoMantenimiento } from '../dominio/modelos/catalogos';
import { DatosModelo, ModeloActivo, TipoControl } from '../dominio/modelos/modelo-activo';
import { RepositorioCatalogosActivos } from '../dominio/repositorios/repositorio-catalogos-activos';

/** Casos de uso de los catalogos del modulo: modelos, categorias, fabricantes y bodegas. */
@Injectable({ providedIn: 'root' })
export class CatalogosActivos {
  private readonly repositorio = inject(RepositorioCatalogosActivos);

  listarCategorias(): Observable<Categoria[]> {
    return this.repositorio.listarCategorias();
  }

  guardarCategoria(id: number | null, nombre: string): Observable<Categoria> {
    return this.repositorio.guardarCategoria(id, nombre.trim());
  }

  listarFabricantes(): Observable<Fabricante[]> {
    return this.repositorio.listarFabricantes();
  }

  guardarFabricante(id: number | null, nombre: string): Observable<Fabricante> {
    return this.repositorio.guardarFabricante(id, nombre.trim());
  }

  listarBodegas(): Observable<Bodega[]> {
    return this.repositorio.listarBodegas();
  }

  guardarBodega(id: number | null, datos: DatosBodega): Observable<Bodega> {
    return this.repositorio.guardarBodega(id, { ...datos, nombre: datos.nombre.trim() });
  }

  listarTiposMantenimiento(): Observable<TipoMantenimiento[]> {
    return this.repositorio.listarTiposMantenimiento();
  }

  listarModelos(tipoControl?: TipoControl): Observable<ModeloActivo[]> {
    return this.repositorio.listarModelos(tipoControl);
  }

  /** Los modelos por lote no llevan intervalo de mantenimiento ni prefijo de codigo. */
  guardarModelo(id: number | null, datos: DatosModelo): Observable<ModeloActivo> {
    const porLote = datos.tipoControl === 'LOTE';
    const prefijo = datos.prefijoCodigo?.trim().toUpperCase();
    return this.repositorio.guardarModelo(id, {
      ...datos,
      nombre: datos.nombre.trim(),
      horasMantenimiento: porLote ? null : datos.horasMantenimiento,
      prefijoCodigo: porLote || !prefijo ? null : prefijo,
    });
  }
}
