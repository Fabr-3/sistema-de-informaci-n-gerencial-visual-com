import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../compartido/modelos/pagina';
import { CatalogosLogistica, DatosPersonal, FiltroPersonal, PersonalTecnico } from '../dominio/modelos/personal';
import { RepositorioPersonal } from '../dominio/repositorios/repositorio-personal';

/** Casos de uso del personal tecnico. */
@Injectable({ providedIn: 'root' })
export class GestionPersonal {
  private readonly repositorio = inject(RepositorioPersonal);

  listar(filtro: FiltroPersonal, solicitud: SolicitudPagina): Observable<Pagina<PersonalTecnico>> {
    const texto = filtro.texto?.trim();
    return this.repositorio.listar({ ...filtro, texto: texto ? texto : undefined }, solicitud);
  }

  /** Recorta los textos antes de guardar; un telefono vacio se guarda como nulo. */
  guardar(id: number | null, datos: DatosPersonal): Observable<PersonalTecnico> {
    return this.repositorio.guardar(id, {
      ...datos,
      nombres: datos.nombres.trim(),
      apellidos: datos.apellidos.trim(),
      telefono: datos.telefono?.trim() || null,
      numeroDocumento: datos.numeroDocumento.trim(),
    });
  }

  catalogos(): Observable<CatalogosLogistica> {
    return this.repositorio.catalogos().pipe(shareReplay({ bufferSize: 1, refCount: true }));
  }
}
