import { Observable } from 'rxjs';

import { AlertaActivo } from '../modelos/alertas';
import { KpisGerenciales } from '../modelos/kpis';
import { TipoReporte } from '../modelos/reportes';

export abstract class RepositorioGerencial {
  /** Fechas ISO (AAAA-MM-DD); sin fechas, el mes en curso. */
  abstract kpis(desde?: string, hasta?: string): Observable<KpisGerenciales>;

  abstract alertasActivos(): Observable<AlertaActivo[]>;

  abstract reporte(tipo: TipoReporte, desde: string, hasta: string): Observable<Blob>;
}
