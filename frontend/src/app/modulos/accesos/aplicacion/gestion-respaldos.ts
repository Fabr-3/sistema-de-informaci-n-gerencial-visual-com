import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Respaldo } from '../dominio/modelos/respaldo';
import { RepositorioRespaldos } from '../dominio/repositorios/repositorio-respaldos';

@Injectable({ providedIn: 'root' })
export class GestionRespaldos {
  private readonly repositorio = inject(RepositorioRespaldos);

  listar(): Observable<Respaldo[]> {
    return this.repositorio.listar();
  }

  generar(): Observable<Respaldo> {
    return this.repositorio.generar();
  }

  descargar(respaldo: Respaldo): Observable<Blob> {
    return this.repositorio.descargar(respaldo.nombreArchivo);
  }
}
