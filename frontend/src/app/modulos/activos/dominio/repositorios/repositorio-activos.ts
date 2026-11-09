import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { Activo, DatosActivo, FiltroActivos, RegistroActivos } from '../modelos/activo';

export abstract class RepositorioActivos {
  abstract listar(filtro: FiltroActivos, solicitud: SolicitudPagina): Observable<Pagina<Activo>>;
  abstract listarAlertasMantenimiento(): Observable<Activo[]>;
  abstract obtener(id: number): Observable<Activo>;
  abstract registrar(registro: RegistroActivos): Observable<Activo[]>;
  abstract actualizar(id: number, datos: DatosActivo): Observable<Activo>;
  abstract darDeBaja(id: number, motivo: string): Observable<Activo>;
  abstract eliminar(id: number): Observable<void>;
}
