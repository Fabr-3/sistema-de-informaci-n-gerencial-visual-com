import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../compartido/modelos/pagina';
import { ElementoCatalogo } from '../../comercial/dominio/modelos/catalogos';
import {
  AjusteConsumible,
  Consumible,
  DatosConsumible,
  FiltroConsumibles,
  IngresoConsumible,
  MovimientoConsumible,
} from '../dominio/modelos/consumible';
import { Consumo, DatosConsumo, EventoAsignado, FiltroConsumos } from '../dominio/modelos/consumo';
import { DatosSubcontratacion, FiltroSubcontrataciones, Subcontratacion } from '../dominio/modelos/subcontratacion';
import {
  RepositorioConsumibles,
  RepositorioConsumos,
  RepositorioSubcontrataciones,
} from '../dominio/repositorios/repositorios-subcontratacion';

/** Subcontrataciones de servicios con su egreso financiero. */
@Injectable({ providedIn: 'root' })
export class GestionSubcontrataciones {
  private readonly repositorio = inject(RepositorioSubcontrataciones);

  listar(filtro: FiltroSubcontrataciones, solicitud: SolicitudPagina): Observable<Pagina<Subcontratacion>> {
    return this.repositorio.listar(filtro, solicitud);
  }

  registrar(datos: DatosSubcontratacion): Observable<Subcontratacion> {
    return this.repositorio.registrar({ ...datos, descripcion: datos.descripcion.trim() });
  }

  /** Anula tambien el egreso vinculado, con el mismo motivo. */
  anular(id: number, motivo: string): Observable<Subcontratacion> {
    return this.repositorio.anular(id, motivo.trim());
  }

  serviciosDelProveedor(idProveedor: number): Observable<ElementoCatalogo[]> {
    return this.repositorio.serviciosDelProveedor(idProveedor);
  }
}

/** Catalogo de consumibles con su stock. */
@Injectable({ providedIn: 'root' })
export class GestionConsumibles {
  private readonly repositorio = inject(RepositorioConsumibles);

  listar(filtro: FiltroConsumibles, solicitud: SolicitudPagina): Observable<Pagina<Consumible>> {
    const texto = filtro.texto?.trim();
    return this.repositorio.listar({ ...filtro, texto: texto ? texto : undefined }, solicitud);
  }

  guardar(id: number | null, datos: DatosConsumible): Observable<Consumible> {
    return this.repositorio.guardar(id, {
      ...datos,
      nombre: datos.nombre.trim(),
      unidadMedida: datos.unidadMedida.trim(),
    });
  }

  ingresar(id: number, ingreso: IngresoConsumible): Observable<Consumible> {
    return this.repositorio.ingresar(id, { ...ingreso, motivo: ingreso.motivo?.trim() || null });
  }

  ajustar(id: number, ajuste: AjusteConsumible): Observable<Consumible> {
    return this.repositorio.ajustar(id, { ...ajuste, motivo: ajuste.motivo.trim() });
  }

  movimientos(id: number, solicitud: SolicitudPagina): Observable<Pagina<MovimientoConsumible>> {
    return this.repositorio.movimientos(id, solicitud);
  }
}

/** Consumos en proyectos y eventos; el tecnico solo opera en sus eventos asignados. */
@Injectable({ providedIn: 'root' })
export class GestionConsumos {
  private readonly repositorio = inject(RepositorioConsumos);

  listar(filtro: FiltroConsumos, solicitud: SolicitudPagina): Observable<Pagina<Consumo>> {
    return this.repositorio.listar(filtro, solicitud);
  }

  registrar(datos: DatosConsumo): Observable<Consumo> {
    return this.repositorio.registrar(datos);
  }

  anular(id: number, motivo: string): Observable<Consumo> {
    return this.repositorio.anular(id, motivo.trim());
  }

  eventosAsignados(): Observable<EventoAsignado[]> {
    return this.repositorio.eventosAsignados();
  }
}
