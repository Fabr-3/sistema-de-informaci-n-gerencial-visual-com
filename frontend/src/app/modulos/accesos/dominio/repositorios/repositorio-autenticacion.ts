import { Observable } from 'rxjs';

import { CambioContrasena, Credenciales, Sesion } from '../modelos/sesion';

/** Puerto de autenticacion. La implementacion HTTP vive en infraestructura. */
export abstract class RepositorioAutenticacion {
  abstract iniciarSesion(credenciales: Credenciales): Observable<Sesion>;
  abstract refrescar(): Observable<Sesion>;
  abstract cerrarSesion(): Observable<void>;
  abstract cambiarContrasena(cambio: CambioContrasena): Observable<Sesion>;
}
