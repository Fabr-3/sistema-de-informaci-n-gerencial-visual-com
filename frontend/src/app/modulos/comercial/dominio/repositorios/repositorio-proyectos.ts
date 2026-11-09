import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import {
  Contrato,
  DatosContrato,
  DatosEvento,
  DatosProyecto,
  DetalleProyecto,
  EstadoEvento,
  EstadoProyecto,
  Evento,
  EventoCalendario,
  FiltroProyectos,
  Proyecto,
} from '../modelos/proyecto';

/** Proyectos con su cronograma de eventos y contratos. */
export abstract class RepositorioProyectos {
  abstract listar(filtro: FiltroProyectos, solicitud: SolicitudPagina): Observable<Pagina<Proyecto>>;
  abstract obtener(id: number): Observable<DetalleProyecto>;
  abstract actualizar(id: number, datos: DatosProyecto): Observable<Proyecto>;
  abstract cambiarEstado(id: number, estado: EstadoProyecto): Observable<Proyecto>;
  abstract guardarEvento(idProyecto: number, idEvento: number | null, datos: DatosEvento): Observable<Evento>;
  abstract cambiarEstadoEvento(idProyecto: number, idEvento: number, estado: EstadoEvento): Observable<Evento>;
  abstract guardarContrato(idProyecto: number, idContrato: number | null, datos: DatosContrato): Observable<Contrato>;
  abstract descargarContrato(idProyecto: number, idContrato: number): Observable<Blob>;
  abstract adjuntarFirmado(idProyecto: number, idContrato: number, archivo: File): Observable<Contrato>;
  abstract descargarFirmado(idProyecto: number, idContrato: number): Observable<Blob>;
  /** Eventos que se cruzan con [desde, hasta) (fechas ISO locales). */
  abstract cronograma(desde: string, hasta: string): Observable<EventoCalendario[]>;
}
