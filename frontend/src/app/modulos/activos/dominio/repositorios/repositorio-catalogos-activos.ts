import { Observable } from 'rxjs';

import { Bodega, Categoria, DatosBodega, Fabricante, TipoMantenimiento } from '../modelos/catalogos';
import { DatosModelo, ModeloActivo, TipoControl } from '../modelos/modelo-activo';

export abstract class RepositorioCatalogosActivos {
  abstract listarCategorias(): Observable<Categoria[]>;
  abstract guardarCategoria(id: number | null, nombre: string): Observable<Categoria>;
  abstract listarFabricantes(): Observable<Fabricante[]>;
  abstract guardarFabricante(id: number | null, nombre: string): Observable<Fabricante>;
  abstract listarBodegas(): Observable<Bodega[]>;
  abstract guardarBodega(id: number | null, datos: DatosBodega): Observable<Bodega>;
  abstract listarTiposMantenimiento(): Observable<TipoMantenimiento[]>;
  abstract listarModelos(tipoControl?: TipoControl): Observable<ModeloActivo[]>;
  abstract guardarModelo(id: number | null, datos: DatosModelo): Observable<ModeloActivo>;
}
