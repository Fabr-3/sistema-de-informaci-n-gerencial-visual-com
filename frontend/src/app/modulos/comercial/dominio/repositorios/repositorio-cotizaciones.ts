import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { Cotizacion, DatosCotizacion, FiltroCotizaciones } from '../modelos/cotizacion';

export abstract class RepositorioCotizaciones {
  abstract listar(filtro: FiltroCotizaciones, solicitud: SolicitudPagina): Observable<Pagina<Cotizacion>>;
  abstract obtener(id: number): Observable<Cotizacion>;
  abstract guardar(id: number | null, datos: DatosCotizacion): Observable<Cotizacion>;
  abstract eliminar(id: number): Observable<void>;
  abstract aceptar(id: number): Observable<Cotizacion>;
  abstract rechazar(id: number, motivo: string): Observable<Cotizacion>;
  abstract descargarPdf(id: number): Observable<Blob>;
}
