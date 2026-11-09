import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../compartido/modelos/pagina';
import { Activo, DatosActivo, FiltroActivos, RegistroActivos } from '../dominio/modelos/activo';
import { RepositorioActivos } from '../dominio/repositorios/repositorio-activos';

/** Casos de uso del registro de activos por unidad. */
@Injectable({ providedIn: 'root' })
export class GestionActivos {
  private readonly repositorio = inject(RepositorioActivos);

  listar(filtro: FiltroActivos, solicitud: SolicitudPagina): Observable<Pagina<Activo>> {
    const texto = filtro.texto?.trim();
    return this.repositorio.listar({ ...filtro, texto: texto ? texto : undefined }, solicitud);
  }

  listarAlertasMantenimiento(): Observable<Activo[]> {
    return this.repositorio.listarAlertasMantenimiento();
  }

  obtener(id: number): Observable<Activo> {
    return this.repositorio.obtener(id);
  }

  /** Registra las unidades; las series vacias se descartan (el backend las exige completas o ninguna). */
  registrar(registro: RegistroActivos): Observable<Activo[]> {
    const codigo = registro.codigoInventario?.trim().toUpperCase();
    const series = registro.numerosSerie.map((serie) => serie.trim()).filter((serie) => serie.length > 0);
    return this.repositorio.registrar({ ...registro, codigoInventario: codigo ? codigo : null, numerosSerie: series });
  }

  actualizar(id: number, datos: DatosActivo): Observable<Activo> {
    return this.repositorio.actualizar(id, datos);
  }

  darDeBaja(id: number, motivo: string): Observable<Activo> {
    return this.repositorio.darDeBaja(id, motivo.trim());
  }

  eliminar(id: number): Observable<void> {
    return this.repositorio.eliminar(id);
  }
}
