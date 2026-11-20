import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { CatalogosProveedores, DatosProveedor, FiltroProveedores, Proveedor } from '../modelos/proveedor';

export abstract class RepositorioProveedores {
  abstract listar(filtro: FiltroProveedores, solicitud: SolicitudPagina): Observable<Pagina<Proveedor>>;
  abstract catalogos(): Observable<CatalogosProveedores>;
  abstract guardar(id: number | null, datos: DatosProveedor): Observable<Proveedor>;
  abstract cambiarEstado(id: number, activo: boolean): Observable<Proveedor>;
  abstract eliminar(id: number): Observable<void>;
}
