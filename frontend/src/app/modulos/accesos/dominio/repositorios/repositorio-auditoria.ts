import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { FiltroAuditoria, RegistroAuditoria } from '../modelos/auditoria';

export abstract class RepositorioAuditoria {
  abstract consultar(filtro: FiltroAuditoria, solicitud: SolicitudPagina): Observable<Pagina<RegistroAuditoria>>;
}
