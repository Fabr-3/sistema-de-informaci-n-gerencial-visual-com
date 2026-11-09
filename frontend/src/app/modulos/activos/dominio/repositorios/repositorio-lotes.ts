import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { Existencia, FiltroExistencias, MovimientoLote, NuevoMovimientoLote, TrasladoLote } from '../modelos/lotes';

export abstract class RepositorioLotes {
  abstract listar(filtro: FiltroExistencias): Observable<Existencia[]>;
  abstract listarMovimientos(idExistencia: number, solicitud: SolicitudPagina): Observable<Pagina<MovimientoLote>>;
  abstract registrarMovimiento(movimiento: NuevoMovimientoLote): Observable<Existencia>;
  abstract trasladar(traslado: TrasladoLote): Observable<Existencia[]>;
}
