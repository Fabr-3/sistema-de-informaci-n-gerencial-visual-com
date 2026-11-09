import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { URL_API } from '../../../../nucleo/configuracion/api';
import { Respaldo } from '../../dominio/modelos/respaldo';
import { RepositorioRespaldos } from '../../dominio/repositorios/repositorio-respaldos';

@Injectable()
export class RepositorioRespaldosHttp extends RepositorioRespaldos {
  private readonly http = inject(HttpClient);
  private readonly url = inject(URL_API) + '/respaldos';

  listar(): Observable<Respaldo[]> {
    return this.http.get<Respaldo[]>(this.url);
  }

  generar(): Observable<Respaldo> {
    return this.http.post<Respaldo>(this.url, null);
  }

  /** Se descarga por HttpClient (no con un enlace) para enviar el token de acceso. */
  descargar(nombreArchivo: string): Observable<Blob> {
    return this.http.get(`${this.url}/${encodeURIComponent(nombreArchivo)}/descarga`, { responseType: 'blob' });
  }
}
