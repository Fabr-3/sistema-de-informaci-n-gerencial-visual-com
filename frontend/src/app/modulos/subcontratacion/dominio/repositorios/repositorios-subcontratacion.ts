import { Observable } from 'rxjs';

import { Pagina, SolicitudPagina } from '../../../../compartido/modelos/pagina';
import { ElementoCatalogo } from '../../../comercial/dominio/modelos/catalogos';
import {
  AjusteConsumible,
  Consumible,
  DatosConsumible,
  FiltroConsumibles,
  IngresoConsumible,
  MovimientoConsumible,
} from '../modelos/consumible';
import { Consumo, DatosConsumo, EventoAsignado, FiltroConsumos } from '../modelos/consumo';
import { DatosGasto, FiltroGastos, GastoEvento } from '../modelos/gasto';
import { DatosSubcontratacion, FiltroSubcontrataciones, Subcontratacion } from '../modelos/subcontratacion';

export abstract class RepositorioSubcontrataciones {
  abstract listar(filtro: FiltroSubcontrataciones, solicitud: SolicitudPagina): Observable<Pagina<Subcontratacion>>;
  abstract registrar(datos: DatosSubcontratacion): Observable<Subcontratacion>;
  abstract anular(id: number, motivo: string): Observable<Subcontratacion>;
  abstract serviciosDelProveedor(idProveedor: number): Observable<ElementoCatalogo[]>;
}

export abstract class RepositorioConsumibles {
  abstract listar(filtro: FiltroConsumibles, solicitud: SolicitudPagina): Observable<Pagina<Consumible>>;
  abstract guardar(id: number | null, datos: DatosConsumible): Observable<Consumible>;
  abstract ingresar(id: number, ingreso: IngresoConsumible): Observable<Consumible>;
  abstract ajustar(id: number, ajuste: AjusteConsumible): Observable<Consumible>;
  abstract movimientos(id: number, solicitud: SolicitudPagina): Observable<Pagina<MovimientoConsumible>>;
}

export abstract class RepositorioConsumos {
  abstract listar(filtro: FiltroConsumos, solicitud: SolicitudPagina): Observable<Pagina<Consumo>>;
  abstract registrar(datos: DatosConsumo): Observable<Consumo>;
  abstract anular(id: number, motivo: string): Observable<Consumo>;
  abstract eventosAsignados(): Observable<EventoAsignado[]>;
}

/** Gastos del evento: el comprobante viaja como archivo junto con los datos. */
export abstract class RepositorioGastos {
  abstract listar(filtro: FiltroGastos, solicitud: SolicitudPagina): Observable<Pagina<GastoEvento>>;
  abstract registrar(datos: DatosGasto, comprobante: File): Observable<GastoEvento>;
  abstract comprobante(id: number): Observable<Blob>;
  abstract anular(id: number, motivo: string): Observable<GastoEvento>;
}
