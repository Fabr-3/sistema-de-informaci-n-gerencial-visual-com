import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../compartido/modelos/pagina';
import {
  EstadoModuloLed,
  FinMantenimiento,
  InicioMantenimiento,
  Mantenimiento,
  ModuloLed,
  NuevoRegistroUso,
  RegistroUso,
} from '../dominio/modelos/historial';
import { RepositorioHistorialActivos } from '../dominio/repositorios/repositorio-historial-activos';

/** Casos de uso del historial de un activo: uso, mantenimiento y modulos LED. */
@Injectable({ providedIn: 'root' })
export class HistorialActivos {
  private readonly repositorio = inject(RepositorioHistorialActivos);

  listarUsos(idActivo: number, solicitud: SolicitudPagina): Observable<Pagina<RegistroUso>> {
    return this.repositorio.listarUsos(idActivo, solicitud);
  }

  registrarUso(idActivo: number, uso: NuevoRegistroUso): Observable<RegistroUso> {
    return this.repositorio.registrarUso(idActivo, uso);
  }

  anularUso(idActivo: number, idRegistro: number): Observable<void> {
    return this.repositorio.anularUso(idActivo, idRegistro);
  }

  listarMantenimientos(idActivo: number, solicitud: SolicitudPagina): Observable<Pagina<Mantenimiento>> {
    return this.repositorio.listarMantenimientos(idActivo, solicitud);
  }

  iniciarMantenimiento(idActivo: number, inicio: InicioMantenimiento): Observable<Mantenimiento> {
    return this.repositorio.iniciarMantenimiento(idActivo, inicio);
  }

  finalizarMantenimiento(idActivo: number, idMantenimiento: number, fin: FinMantenimiento): Observable<Mantenimiento> {
    return this.repositorio.finalizarMantenimiento(idActivo, idMantenimiento, fin);
  }

  listarModulos(idActivo: number): Observable<ModuloLed[]> {
    return this.repositorio.listarModulos(idActivo);
  }

  agregarModulo(idActivo: number, codigo: string): Observable<ModuloLed> {
    return this.repositorio.agregarModulo(idActivo, codigo.trim().toUpperCase());
  }

  cambiarEstadoModulo(idActivo: number, idModulo: number, estado: EstadoModuloLed): Observable<ModuloLed> {
    return this.repositorio.cambiarEstadoModulo(idActivo, idModulo, estado);
  }
}
