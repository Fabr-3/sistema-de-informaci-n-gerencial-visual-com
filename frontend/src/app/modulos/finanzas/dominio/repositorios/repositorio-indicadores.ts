import { Observable } from 'rxjs';

import { IndicadoresFinancieros } from '../modelos/indicadores';

export abstract class RepositorioIndicadores {
  /** Fechas ISO (AAAA-MM-DD); sin fechas, el mes en curso. */
  abstract calcular(desde?: string, hasta?: string): Observable<IndicadoresFinancieros>;
}
