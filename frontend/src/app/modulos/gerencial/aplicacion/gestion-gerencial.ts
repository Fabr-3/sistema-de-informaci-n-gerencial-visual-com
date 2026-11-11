import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AlertaActivo } from '../dominio/modelos/alertas';
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

  alertasActivos(): Observable<AlertaActivo[]> {
    return this.repositorio.alertasActivos();
  }

  reporte(tipo: TipoReporte, desde: string, hasta: string): Observable<Blob> {
    return this.repositorio.reporte(tipo, desde, hasta);
  }
}
