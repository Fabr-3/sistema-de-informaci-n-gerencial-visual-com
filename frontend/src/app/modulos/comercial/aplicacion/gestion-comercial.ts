import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../compartido/modelos/pagina';
import { DatosLugar, DatosServicio, ElementoCatalogo, Lugar, Servicio } from '../dominio/modelos/catalogos';
import { Cliente, DatosCliente, FiltroClientes } from '../dominio/modelos/cliente';
import { RepositorioCatalogosComerciales } from '../dominio/repositorios/repositorio-catalogos-comerciales';
import { RepositorioClientes } from '../dominio/repositorios/repositorio-clientes';

/** Casos de uso de clientes y de los catalogos comerciales (servicios y lugares). */
@Injectable({ providedIn: 'root' })
export class GestionComercial {
  private readonly clientes = inject(RepositorioClientes);
  private readonly catalogos = inject(RepositorioCatalogosComerciales);

  listarClientes(filtro: FiltroClientes, solicitud: SolicitudPagina): Observable<Pagina<Cliente>> {
    const texto = filtro.texto?.trim();
    return this.clientes.listar({ ...filtro, texto: texto ? texto : undefined }, solicitud);
  }

  obtenerCliente(id: number): Observable<Cliente> {
    return this.clientes.obtener(id);
  }

  /** Recorta los textos y descarta contactos vacios antes de guardar. */
  guardarCliente(id: number | null, datos: DatosCliente): Observable<Cliente> {
    return this.clientes.guardar(id, {
      ...datos,
      razonSocial: datos.razonSocial.trim(),
      direccion: datos.direccion?.trim() || null,
      documentos: datos.documentos.map((documento) => ({ ...documento, numero: documento.numero.trim() })),
      contactos: datos.contactos
        .filter((contacto) => contacto.valor.trim().length > 0)
        .map((contacto) => ({ ...contacto, valor: contacto.valor.trim(), referencia: contacto.referencia?.trim() || null })),
    });
  }

  eliminarCliente(id: number): Observable<void> {
    return this.clientes.eliminar(id);
  }

  listarTiposCliente(): Observable<ElementoCatalogo[]> {
    return this.catalogos.listarTiposCliente();
  }

  listarTiposDocumento(): Observable<ElementoCatalogo[]> {
    return this.catalogos.listarTiposDocumento();
  }

  listarTiposContacto(): Observable<ElementoCatalogo[]> {
    return this.catalogos.listarTiposContacto();
  }

  listarServicios(soloActivos = false): Observable<Servicio[]> {
    return this.catalogos.listarServicios(soloActivos);
  }

  guardarServicio(id: number | null, datos: DatosServicio): Observable<Servicio> {
    return this.catalogos.guardarServicio(id, {
      ...datos,
      nombre: datos.nombre.trim(),
      unidadMedida: datos.unidadMedida.trim(),
      descripcion: datos.descripcion?.trim() || null,
    });
  }

  listarLugares(): Observable<Lugar[]> {
    return this.catalogos.listarLugares();
  }

  guardarLugar(id: number | null, datos: DatosLugar): Observable<Lugar> {
    return this.catalogos.guardarLugar(id, {
      nombre: datos.nombre.trim(),
      direccion: datos.direccion.trim(),
      ciudad: datos.ciudad?.trim() || null,
    });
  }
}
