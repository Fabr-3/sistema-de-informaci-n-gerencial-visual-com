import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { IndicadoresFinancieros } from '../dominio/modelos/indicadores';
import { RepositorioIndicadores } from '../dominio/repositorios/repositorio-indicadores';

/** Tablero financiero del gerente. */
@Injectable({ providedIn: 'root' })
export class GestionIndicadores {
  private readonly repositorio = inject(RepositorioIndicadores);

  calcular(desde?: string, hasta?: string): Observable<IndicadoresFinancieros> {
    return this.repositorio.calcular(desde, hasta);
  }
}
