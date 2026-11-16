import { Observable } from 'rxjs';

import { ActivoReservable, FiltroActivosReservables, Reserva } from '../modelos/reservas';

/** Reserva de equipos para el plan logistico de un evento. */
export abstract class RepositorioReservas {
  abstract listar(idEvento: number): Observable<Reserva[]>;
  abstract disponibles(idEvento: number, filtro: FiltroActivosReservables): Observable<ActivoReservable[]>;
  abstract reservar(idEvento: number, idsActivos: readonly number[]): Observable<Reserva[]>;
  abstract liberar(idEvento: number, idReserva: number, motivo: string): Observable<Reserva>;
}
