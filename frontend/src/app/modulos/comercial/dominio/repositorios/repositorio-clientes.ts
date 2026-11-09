import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { Cliente, DatosCliente, FiltroClientes } from '../modelos/cliente';

export abstract class RepositorioClientes {
  abstract listar(filtro: FiltroClientes, solicitud: SolicitudPagina): Observable<Pagina<Cliente>>;
  abstract obtener(id: number): Observable<Cliente>;
  abstract guardar(id: number | null, datos: DatosCliente): Observable<Cliente>;
  abstract eliminar(id: number): Observable<void>;
}
