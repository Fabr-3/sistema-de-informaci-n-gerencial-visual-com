import { Observable } from 'rxjs';

import { DatosLugar, DatosServicio, ElementoCatalogo, Lugar, Servicio } from '../modelos/catalogos';

export abstract class RepositorioCatalogosComerciales {
  abstract listarTiposCliente(): Observable<ElementoCatalogo[]>;
  abstract listarTiposDocumento(): Observable<ElementoCatalogo[]>;
  abstract listarTiposContacto(): Observable<ElementoCatalogo[]>;
  abstract listarServicios(soloActivos: boolean): Observable<Servicio[]>;
  abstract guardarServicio(id: number | null, datos: DatosServicio): Observable<Servicio>;
  abstract listarLugares(): Observable<Lugar[]>;
  abstract guardarLugar(id: number | null, datos: DatosLugar): Observable<Lugar>;
}
