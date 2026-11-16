import { Observable } from 'rxjs';

import { Alerta, EstadoAlerta } from '../modelos/alertas';
import { KpisGerenciales } from '../modelos/kpis';
import { TipoReporte } from '../modelos/reportes';

export abstract class RepositorioGerencial {
  /** Fechas ISO (AAAA-MM-DD); sin fechas, el mes en curso. */
  abstract kpis(desde?: string, hasta?: string): Observable<KpisGerenciales>;

  /** Sincroniza las alertas con la situacion actual y devuelve las del estado pedido. */
  abstract alertas(estado: EstadoAlerta): Observable<Alerta[]>;

  abstract atenderAlerta(id: number, comentario: string): Observable<Alerta>;

  abstract reporte(tipo: TipoReporte, desde: string, hasta: string): Observable<Blob>;
}
