import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../compartido/modelos/pagina';
import {
  EntradaHistorialModulo,
  EstadoModuloLed,
  FinMantenimiento,
  InicioMantenimiento,
  Mantenimiento,
  ModuloLed,
  NuevoRegistroUso,
  ReasignacionModulo,
  ReemplazoModulo,
  RegistroUso,
  ReservaVigente,
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

  registrarUsoGrupal(idsActivos: readonly number[], uso: NuevoRegistroUso): Observable<RegistroUso[]> {
    return this.repositorio.registrarUsoGrupal(idsActivos, uso);
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

  /** El codigo nuevo se normaliza igual que al agregar un modulo. */
  reemplazarModulo(idActivo: number, idModulo: number, datos: ReemplazoModulo): Observable<ModuloLed> {
    return this.repositorio.reemplazarModulo(idActivo, idModulo, {
      codigoNuevo: datos.codigoNuevo.trim().toUpperCase(),
      motivo: datos.motivo.trim(),
    });
  }

  reasignarModulo(idActivo: number, idModulo: number, datos: ReasignacionModulo): Observable<ModuloLed> {
    return this.repositorio.reasignarModulo(idActivo, idModulo, { ...datos, motivo: datos.motivo.trim() });
  }

  historialModulo(idActivo: number, idModulo: number): Observable<EntradaHistorialModulo[]> {
    return this.repositorio.historialModulo(idActivo, idModulo);
  }

  /** Proximas reservas del equipo para eventos (solo lectura). */
  listarReservas(idActivo: number): Observable<ReservaVigente[]> {
    return this.repositorio.listarReservas(idActivo);
  }
}
