import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../compartido/modelos/pagina';
import { Cotizacion, DatosCotizacion, FiltroCotizaciones } from '../dominio/modelos/cotizacion';
import { RepositorioCotizaciones } from '../dominio/repositorios/repositorio-cotizaciones';

/** Casos de uso de cotizaciones: borrador, aceptacion o rechazo por gerencia y PDF. */
@Injectable({ providedIn: 'root' })
export class GestionCotizaciones {
  private readonly repositorio = inject(RepositorioCotizaciones);

  listar(filtro: FiltroCotizaciones, solicitud: SolicitudPagina): Observable<Pagina<Cotizacion>> {
    const texto = filtro.texto?.trim();
    return this.repositorio.listar({ ...filtro, texto: texto ? texto : undefined }, solicitud);
  }

  obtener(id: number): Observable<Cotizacion> {
    return this.repositorio.obtener(id);
  }

  guardar(id: number | null, datos: DatosCotizacion): Observable<Cotizacion> {
    return this.repositorio.guardar(id, {
      ...datos,
      titulo: datos.titulo.trim(),
      condiciones: datos.condiciones?.trim() || null,
    });
  }

  eliminar(id: number): Observable<void> {
    return this.repositorio.eliminar(id);
  }

  aceptar(id: number): Observable<Cotizacion> {
    return this.repositorio.aceptar(id);
  }

  rechazar(id: number, motivo: string): Observable<Cotizacion> {
    return this.repositorio.rechazar(id, motivo.trim());
  }

  descargarPdf(id: number): Observable<Blob> {
    return this.repositorio.descargarPdf(id);
  }
}
