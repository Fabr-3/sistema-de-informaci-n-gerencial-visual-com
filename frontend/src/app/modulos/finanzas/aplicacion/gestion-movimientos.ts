import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';

import { SolicitudPagina } from '../../../compartido/modelos/pagina';
import {
  DatosMovimiento,
  FiltroMovimientos,
  ListadoMovimientos,
  Movimiento,
  TipoMovimiento,
} from '../dominio/modelos/movimiento';
import { RepositorioMovimientos } from '../dominio/repositorios/repositorio-movimientos';

/** Casos de uso de ingresos y egresos. */
@Injectable({ providedIn: 'root' })
export class GestionMovimientos {
  private readonly repositorio = inject(RepositorioMovimientos);
  private readonly tiposCompartidos = this.repositorio.tipos().pipe(shareReplay({ bufferSize: 1, refCount: false }));

  tipos(): Observable<TipoMovimiento[]> {
    return this.tiposCompartidos;
  }

  listar(filtro: FiltroMovimientos, solicitud: SolicitudPagina): Observable<ListadoMovimientos> {
    const texto = filtro.texto?.trim();
    return this.repositorio.listar({ ...filtro, texto: texto ? texto : undefined }, solicitud);
  }

  /** Recorta los textos; la factura vacia se envia como nula. */
  registrar(datos: DatosMovimiento): Observable<Movimiento> {
    return this.repositorio.registrar({
      ...datos,
      concepto: datos.concepto.trim(),
      numeroFactura: datos.numeroFactura?.trim() || null,
    });
  }

  anular(id: number, motivo: string): Observable<Movimiento> {
    return this.repositorio.anular(id, motivo.trim());
  }
}
