import { Observable } from 'rxjs';

import { SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { DatosMovimiento, FiltroMovimientos, ListadoMovimientos, Movimiento, TipoMovimiento } from '../modelos/movimiento';

export abstract class RepositorioMovimientos {
  abstract tipos(): Observable<TipoMovimiento[]>;
  abstract listar(filtro: FiltroMovimientos, solicitud: SolicitudPagina): Observable<ListadoMovimientos>;
  abstract registrar(datos: DatosMovimiento): Observable<Movimiento>;
  abstract anular(id: number, motivo: string): Observable<Movimiento>;
}
