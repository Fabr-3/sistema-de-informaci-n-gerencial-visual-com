import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** POST a la coleccion para crear (id nulo) o PUT al recurso para actualizar. */
export function guardarRecurso<T>(http: HttpClient, url: string, id: number | null, cuerpo: object): Observable<T> {
  return id === null ? http.post<T>(url, cuerpo) : http.put<T>(`${url}/${id}`, cuerpo);
}
