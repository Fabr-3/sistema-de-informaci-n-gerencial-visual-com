import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../compartido/modelos/pagina';
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
} from '../dominio/modelos/proyecto';
import { RepositorioProyectos } from '../dominio/repositorios/repositorio-proyectos';

/** Casos de uso de proyectos, su cronograma de eventos y sus contratos. */
@Injectable({ providedIn: 'root' })
export class GestionProyectos {
  private readonly repositorio = inject(RepositorioProyectos);

  listar(filtro: FiltroProyectos, solicitud: SolicitudPagina): Observable<Pagina<Proyecto>> {
    const texto = filtro.texto?.trim();
    return this.repositorio.listar({ ...filtro, texto: texto ? texto : undefined }, solicitud);
  }

  obtener(id: number): Observable<DetalleProyecto> {
    return this.repositorio.obtener(id);
  }

  actualizar(id: number, datos: DatosProyecto): Observable<Proyecto> {
    return this.repositorio.actualizar(id, {
      ...datos,
      nombre: datos.nombre.trim(),
      descripcion: datos.descripcion?.trim() || null,
    });
  }

  cambiarEstado(id: number, estado: EstadoProyecto): Observable<Proyecto> {
    return this.repositorio.cambiarEstado(id, estado);
  }

  guardarEvento(idProyecto: number, idEvento: number | null, datos: DatosEvento): Observable<Evento> {
    return this.repositorio.guardarEvento(idProyecto, idEvento, { ...datos, nombre: datos.nombre.trim() });
  }

  cambiarEstadoEvento(idProyecto: number, idEvento: number, estado: EstadoEvento): Observable<Evento> {
    return this.repositorio.cambiarEstadoEvento(idProyecto, idEvento, estado);
  }

  guardarContrato(idProyecto: number, idContrato: number | null, datos: DatosContrato): Observable<Contrato> {
    return this.repositorio.guardarContrato(idProyecto, idContrato, {
      ...datos,
      terminos: datos.terminos?.trim() || null,
    });
  }

  descargarContrato(idProyecto: number, idContrato: number): Observable<Blob> {
    return this.repositorio.descargarContrato(idProyecto, idContrato);
  }

  adjuntarFirmado(idProyecto: number, idContrato: number, archivo: File): Observable<Contrato> {
    return this.repositorio.adjuntarFirmado(idProyecto, idContrato, archivo);
  }

  descargarFirmado(idProyecto: number, idContrato: number): Observable<Blob> {
    return this.repositorio.descargarFirmado(idProyecto, idContrato);
  }

  cronograma(desde: string, hasta: string): Observable<EventoCalendario[]> {
    return this.repositorio.cronograma(desde, hasta);
  }
}
