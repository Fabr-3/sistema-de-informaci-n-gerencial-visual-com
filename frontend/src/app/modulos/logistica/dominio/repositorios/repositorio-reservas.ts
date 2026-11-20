import { Observable } from 'rxjs';

import {
  ActivoReservable,
  DisponibilidadModelo,
  EquipoSubalquilado,
  FiltroActivosReservables,
  PedidoModelo,
  Reserva,
} from '../modelos/reservas';

/** Reserva de equipos para el plan logistico de un evento. */
export abstract class RepositorioReservas {
  abstract listar(idEvento: number): Observable<Reserva[]>;
  abstract disponibles(idEvento: number, filtro: FiltroActivosReservables): Observable<ActivoReservable[]>;
  abstract reservar(idEvento: number, idsActivos: readonly number[]): Observable<Reserva[]>;
  abstract disponibilidadPorModelo(idEvento: number): Observable<DisponibilidadModelo[]>;
  abstract reservarAutomaticamente(idEvento: number, pedidos: readonly PedidoModelo[]): Observable<Reserva[]>;
  abstract subalquileres(idEvento: number): Observable<EquipoSubalquilado[]>;
  abstract liberar(idEvento: number, idReserva: number, motivo: string): Observable<Reserva>;
}
