import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../compartido/modelos/pagina';
import { FiltroAuditoria, RegistroAuditoria } from '../dominio/modelos/auditoria';
import { RepositorioAuditoria } from '../dominio/repositorios/repositorio-auditoria';

@Injectable({ providedIn: 'root' })
export class ConsultaAuditoria {
  private readonly repositorio = inject(RepositorioAuditoria);

  consultar(filtro: FiltroAuditoria, solicitud: SolicitudPagina): Observable<Pagina<RegistroAuditoria>> {
    const entidad = filtro.entidad?.trim();
    return this.repositorio.consultar({ ...filtro, entidad: entidad ? entidad : undefined }, solicitud);
  }
}
