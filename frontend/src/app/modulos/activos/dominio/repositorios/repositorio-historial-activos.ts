import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
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
} from '../modelos/historial';

/** Historial de un activo: usos, mantenimientos y modulos LED. */
export abstract class RepositorioHistorialActivos {
  abstract listarUsos(idActivo: number, solicitud: SolicitudPagina): Observable<Pagina<RegistroUso>>;
  abstract registrarUso(idActivo: number, uso: NuevoRegistroUso): Observable<RegistroUso>;
  abstract anularUso(idActivo: number, idRegistro: number): Observable<void>;
  abstract listarMantenimientos(idActivo: number, solicitud: SolicitudPagina): Observable<Pagina<Mantenimiento>>;
  abstract iniciarMantenimiento(idActivo: number, inicio: InicioMantenimiento): Observable<Mantenimiento>;
  abstract finalizarMantenimiento(idActivo: number, idMantenimiento: number, fin: FinMantenimiento): Observable<Mantenimiento>;
  abstract listarModulos(idActivo: number): Observable<ModuloLed[]>;
  abstract agregarModulo(idActivo: number, codigo: string): Observable<ModuloLed>;
  abstract reemplazarModulo(idActivo: number, idModulo: number, datos: ReemplazoModulo): Observable<ModuloLed>;
  abstract reasignarModulo(idActivo: number, idModulo: number, datos: ReasignacionModulo): Observable<ModuloLed>;
  abstract historialModulo(idActivo: number, idModulo: number): Observable<EntradaHistorialModulo[]>;
  abstract listarReservas(idActivo: number): Observable<ReservaVigente[]>;
  abstract cambiarEstadoModulo(idActivo: number, idModulo: number, estado: EstadoModuloLed): Observable<ModuloLed>;
}
