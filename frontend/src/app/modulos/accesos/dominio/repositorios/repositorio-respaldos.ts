import { Observable } from 'rxjs';

import { Respaldo } from '../modelos/respaldo';

export abstract class RepositorioRespaldos {
  abstract listar(): Observable<Respaldo[]>;
  abstract generar(): Observable<Respaldo>;
  abstract descargar(nombreArchivo: string): Observable<Blob>;
}
