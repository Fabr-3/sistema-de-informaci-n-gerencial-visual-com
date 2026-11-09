import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { CatalogosLogistica, DatosPersonal, FiltroPersonal, PersonalTecnico } from '../modelos/personal';

export abstract class RepositorioPersonal {
  abstract listar(filtro: FiltroPersonal, solicitud: SolicitudPagina): Observable<Pagina<PersonalTecnico>>;
  abstract guardar(id: number | null, datos: DatosPersonal): Observable<PersonalTecnico>;
  abstract catalogos(): Observable<CatalogosLogistica>;
}
