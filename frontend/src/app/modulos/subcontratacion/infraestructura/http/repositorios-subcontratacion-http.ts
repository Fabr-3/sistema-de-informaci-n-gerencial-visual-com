import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { guardarRecurso } from '../../../../compartido/utilidades/guardar-recurso';
import { parametrosConsulta } from '../../../../compartido/utilidades/parametros-http';
import { URL_API } from '../../../../nucleo/configuracion/api';
import { ElementoCatalogo } from '../../../comercial/dominio/modelos/catalogos';
import {
  AjusteConsumible,
  Consumible,
  DatosConsumible,
  FiltroConsumibles,
  IngresoConsumible,
  MovimientoConsumible,
} from '../../dominio/modelos/consumible';
import { Consumo, DatosConsumo, EventoAsignado, FiltroConsumos } from '../../dominio/modelos/consumo';
import { DatosSubcontratacion, FiltroSubcontrataciones, Subcontratacion } from '../../dominio/modelos/subcontratacion';
import {
  RepositorioConsumibles,
  RepositorioConsumos,
  RepositorioSubcontrataciones,
} from '../../dominio/repositorios/repositorios-subcontratacion';

const RUTA_MODULO = '/subcontratacion';

@Injectable()
export class RepositorioSubcontratacionesHttp extends RepositorioSubcontrataciones {
  private readonly http = inject(HttpClient);
  private readonly base = inject(URL_API) + RUTA_MODULO;
  private readonly url = this.base + '/subcontrataciones';

  listar(filtro: FiltroSubcontrataciones, solicitud: SolicitudPagina): Observable<Pagina<Subcontratacion>> {
    return this.http.get<Pagina<Subcontratacion>>(this.url, { params: parametrosConsulta({ ...filtro, ...solicitud }) });
  }

  registrar(datos: DatosSubcontratacion): Observable<Subcontratacion> {
    return this.http.post<Subcontratacion>(this.url, datos);
  }

  anular(id: number, motivo: string): Observable<Subcontratacion> {
    return this.http.post<Subcontratacion>(`${this.url}/${id}/anulacion`, { motivo });
  }

  serviciosDelProveedor(idProveedor: number): Observable<ElementoCatalogo[]> {
    return this.http.get<ElementoCatalogo[]>(`${this.base}/proveedores/${idProveedor}/servicios`);
  }
}

@Injectable()
export class RepositorioConsumiblesHttp extends RepositorioConsumibles {
  private readonly http = inject(HttpClient);
  private readonly url = inject(URL_API) + RUTA_MODULO + '/consumibles';

  listar(filtro: FiltroConsumibles, solicitud: SolicitudPagina): Observable<Pagina<Consumible>> {
    return this.http.get<Pagina<Consumible>>(this.url, { params: parametrosConsulta({ ...filtro, ...solicitud }) });
  }

  guardar(id: number | null, datos: DatosConsumible): Observable<Consumible> {
    return guardarRecurso<Consumible>(this.http, this.url, id, datos);
  }

  ingresar(id: number, ingreso: IngresoConsumible): Observable<Consumible> {
    return this.http.post<Consumible>(`${this.url}/${id}/ingresos`, ingreso);
  }

  ajustar(id: number, ajuste: AjusteConsumible): Observable<Consumible> {
    return this.http.post<Consumible>(`${this.url}/${id}/ajustes`, ajuste);
  }

  movimientos(id: number, solicitud: SolicitudPagina): Observable<Pagina<MovimientoConsumible>> {
    return this.http.get<Pagina<MovimientoConsumible>>(`${this.url}/${id}/movimientos`, {
      params: parametrosConsulta({ ...solicitud }),
    });
  }
}

@Injectable()
export class RepositorioConsumosHttp extends RepositorioConsumos {
  private readonly http = inject(HttpClient);
  private readonly url = inject(URL_API) + RUTA_MODULO + '/consumos';

  listar(filtro: FiltroConsumos, solicitud: SolicitudPagina): Observable<Pagina<Consumo>> {
    return this.http.get<Pagina<Consumo>>(this.url, { params: parametrosConsulta({ ...filtro, ...solicitud }) });
  }

  registrar(datos: DatosConsumo): Observable<Consumo> {
    return this.http.post<Consumo>(this.url, datos);
  }

  anular(id: number, motivo: string): Observable<Consumo> {
    return this.http.post<Consumo>(`${this.url}/${id}/anulacion`, { motivo });
  }

  eventosAsignados(): Observable<EventoAsignado[]> {
    return this.http.get<EventoAsignado[]>(`${this.url}/eventos-asignados`);
  }
}
