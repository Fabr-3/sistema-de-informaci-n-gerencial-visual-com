import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { DetalleResultadoProyecto, FiltroResultados, ResultadoProyecto } from '../modelos/resultado';

export abstract class RepositorioResultados {
  abstract listar(filtro: FiltroResultados, solicitud: SolicitudPagina): Observable<Pagina<ResultadoProyecto>>;
  abstract detalle(idProyecto: number): Observable<DetalleResultadoProyecto>;
}
