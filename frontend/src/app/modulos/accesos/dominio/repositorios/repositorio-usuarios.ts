import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { DatosUsuario, FiltroUsuarios, NuevoUsuario, Usuario } from '../modelos/usuario';

export abstract class RepositorioUsuarios {
  abstract listar(filtro: FiltroUsuarios, solicitud: SolicitudPagina): Observable<Pagina<Usuario>>;
  abstract crear(usuario: NuevoUsuario): Observable<Usuario>;
  abstract actualizar(id: number, datos: DatosUsuario): Observable<Usuario>;
  abstract cambiarEstado(id: number, activo: boolean): Observable<Usuario>;
  abstract restablecerContrasena(id: number, contrasenaTemporal: string): Observable<void>;
  abstract eliminar(id: number): Observable<void>;
}
