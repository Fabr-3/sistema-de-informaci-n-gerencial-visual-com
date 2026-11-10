import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../compartido/modelos/pagina';
import { DatosProveedor, FiltroProveedores, Proveedor } from '../dominio/modelos/proveedor';
import { RepositorioProveedores } from '../dominio/repositorios/repositorio-proveedores';

/** Casos de uso del registro de proveedores. */
@Injectable({ providedIn: 'root' })
export class GestionProveedores {
  private readonly repositorio = inject(RepositorioProveedores);

  listar(filtro: FiltroProveedores, solicitud: SolicitudPagina): Observable<Pagina<Proveedor>> {
    const texto = filtro.texto?.trim();
    return this.repositorio.listar({ ...filtro, texto: texto ? texto : undefined }, solicitud);
  }

  /** Recorta los textos; los campos opcionales vacios se envian como nulos. */
  guardar(id: number | null, datos: DatosProveedor): Observable<Proveedor> {
    return this.repositorio.guardar(id, {
      razonSocial: datos.razonSocial.trim(),
      nit: datos.nit?.trim() || null,
      telefono: datos.telefono?.trim() || null,
      email: datos.email?.trim() || null,
      servicios: datos.servicios,
    });
  }

  eliminar(id: number): Observable<void> {
    return this.repositorio.eliminar(id);
  }
}
