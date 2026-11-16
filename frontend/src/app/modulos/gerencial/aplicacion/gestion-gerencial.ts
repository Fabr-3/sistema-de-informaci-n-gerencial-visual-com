import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Alerta, EstadoAlerta } from '../dominio/modelos/alertas';
import { KpisGerenciales } from '../dominio/modelos/kpis';
import { TipoReporte } from '../dominio/modelos/reportes';
import { RepositorioGerencial } from '../dominio/repositorios/repositorio-gerencial';

/** KPIs, alertas de activos y reportes operativos del gerente. */
@Injectable({ providedIn: 'root' })
export class GestionGerencial {
  private readonly repositorio = inject(RepositorioGerencial);

  kpis(desde?: string, hasta?: string): Observable<KpisGerenciales> {
    return this.repositorio.kpis(desde, hasta);
  }

  alertas(estado: EstadoAlerta): Observable<Alerta[]> {
    return this.repositorio.alertas(estado);
  }

  atenderAlerta(id: number, comentario: string): Observable<Alerta> {
    return this.repositorio.atenderAlerta(id, comentario.trim());
  }

  reporte(tipo: TipoReporte, desde: string, hasta: string): Observable<Blob> {
    return this.repositorio.reporte(tipo, desde, hasta);
  }
}
