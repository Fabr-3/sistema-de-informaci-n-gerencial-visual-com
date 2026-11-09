import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../compartido/modelos/pagina';
import { DatosUsuario, FiltroUsuarios, NuevoUsuario, Usuario } from '../dominio/modelos/usuario';
import { RepositorioUsuarios } from '../dominio/repositorios/repositorio-usuarios';

/** Casos de uso de administracion de cuentas de usuario. */
@Injectable({ providedIn: 'root' })
export class GestionUsuarios {
  private readonly repositorio = inject(RepositorioUsuarios);

  listar(filtro: FiltroUsuarios, solicitud: SolicitudPagina): Observable<Pagina<Usuario>> {
    const texto = filtro.texto?.trim();
    return this.repositorio.listar({ ...filtro, texto: texto ? texto : undefined }, solicitud);
  }

  crear(usuario: NuevoUsuario): Observable<Usuario> {
    return this.repositorio.crear({ ...usuario, nombreUsuario: usuario.nombreUsuario.trim().toLowerCase() });
  }

  actualizar(id: number, datos: DatosUsuario): Observable<Usuario> {
    return this.repositorio.actualizar(id, datos);
  }

  cambiarEstado(usuario: Usuario): Observable<Usuario> {
    return this.repositorio.cambiarEstado(usuario.id, !usuario.activo);
  }

  restablecerContrasena(id: number, contrasenaTemporal: string): Observable<void> {
    return this.repositorio.restablecerContrasena(id, contrasenaTemporal);
  }

  eliminar(id: number): Observable<void> {
    return this.repositorio.eliminar(id);
  }
}
