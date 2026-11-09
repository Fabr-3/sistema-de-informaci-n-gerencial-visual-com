import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../compartido/modelos/pagina';
import {
  Existencia,
  FiltroExistencias,
  MovimientoLote,
  NuevoMovimientoLote,
  TrasladoLote,
} from '../dominio/modelos/lotes';
import { RepositorioLotes } from '../dominio/repositorios/repositorio-lotes';

@Injectable({ providedIn: 'root' })
export class InventarioLotes {
  private readonly repositorio = inject(RepositorioLotes);

  listar(filtro: FiltroExistencias): Observable<Existencia[]> {
    const texto = filtro.texto?.trim();
    return this.repositorio.listar({ ...filtro, texto: texto ? texto : undefined });
  }

  listarMovimientos(idExistencia: number, solicitud: SolicitudPagina): Observable<Pagina<MovimientoLote>> {
    return this.repositorio.listarMovimientos(idExistencia, solicitud);
  }

  registrarMovimiento(movimiento: NuevoMovimientoLote): Observable<Existencia> {
    return this.repositorio.registrarMovimiento(movimiento);
  }

  trasladar(traslado: TrasladoLote): Observable<Existencia[]> {
    return this.repositorio.trasladar(traslado);
  }
}
