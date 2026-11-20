import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../compartido/modelos/pagina';
import { CatalogosProveedores, DatosProveedor, FiltroProveedores, Proveedor } from '../dominio/modelos/proveedor';
import { RepositorioProveedores } from '../dominio/repositorios/repositorio-proveedores';

/** Casos de uso del registro de proveedores. */
@Injectable({ providedIn: 'root' })
export class GestionProveedores {
  private readonly repositorio = inject(RepositorioProveedores);

  listar(filtro: FiltroProveedores, solicitud: SolicitudPagina): Observable<Pagina<Proveedor>> {
    const texto = filtro.texto?.trim();
    return this.repositorio.listar({ ...filtro, texto: texto ? texto : undefined }, solicitud);
  }

  catalogos(): Observable<CatalogosProveedores> {
    return this.repositorio.catalogos();
  }

  /** Anulado, el proveedor no se elige en movimientos ni subcontrataciones nuevos. */
  cambiarEstado(id: number, activo: boolean): Observable<Proveedor> {
    return this.repositorio.cambiarEstado(id, activo);
  }

  /** Recorta los textos; los campos opcionales vacios se envian como nulos. */
  guardar(id: number | null, datos: DatosProveedor): Observable<Proveedor> {
    return this.repositorio.guardar(id, {
      idTipoProveedor: datos.idTipoProveedor,
      razonSocial: datos.razonSocial.trim(),
      direccion: datos.direccion?.trim() || null,
      documentos: datos.documentos.map((documento) => ({
        ...documento,
        numero: documento.numero.trim(),
        complemento: documento.complemento?.trim().toUpperCase() || null,
      })),
      telefono: datos.telefono?.trim() || null,
      email: datos.email?.trim() || null,
      servicios: datos.servicios,
    });
  }

  eliminar(id: number): Observable<void> {
    return this.repositorio.eliminar(id);
  }
}
