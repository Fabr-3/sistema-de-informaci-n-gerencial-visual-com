import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../compartido/modelos/pagina';
import { DetalleResultadoProyecto, FiltroResultados, ResultadoProyecto } from '../dominio/modelos/resultado';
import { RepositorioResultados } from '../dominio/repositorios/repositorio-resultados';

/** Consulta de costos y margenes por proyecto y evento. */
@Injectable({ providedIn: 'root' })
export class GestionResultados {
  private readonly repositorio = inject(RepositorioResultados);

  listar(filtro: FiltroResultados, solicitud: SolicitudPagina): Observable<Pagina<ResultadoProyecto>> {
    const texto = filtro.texto?.trim();
    return this.repositorio.listar({ ...filtro, texto: texto ? texto : undefined }, solicitud);
  }

  detalle(idProyecto: number): Observable<DetalleResultadoProyecto> {
    return this.repositorio.detalle(idProyecto);
  }
}
